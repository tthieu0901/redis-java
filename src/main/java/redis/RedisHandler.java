package redis;

import protocol.Protocol;
import redis.internal.RedisListCore;
import redis.internal.RedisStringCore;
import redis.processor.RedisWriteProcessor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RedisHandler {
    private final RedisStringCore redisStringCore;
    private final RedisListCore redisListCore;
    private final OutputStream outputStream;

    public RedisHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.redisStringCore = RedisStringCore.getInstance();
        this.redisListCore = RedisListCore.getInstance();
    }

    public void handleCommand(List<Object> request) throws IOException {
        var req = request.stream().filter(Objects::nonNull).map(Object::toString).toList();
        var cmd = getCmd(req);
        switch (cmd) {
            case PING -> ping();
            case ECHO -> echo(req);
            case SET -> set(req);
            case GET -> get(req);
            case RPUSH -> rpush(req);
            case LPUSH -> lpush(req);
            case LRANGE -> lrange(req);
            case LLEN -> llen(req);
            case LPOP -> lpop(req);
            case BLPOP -> blpop(req);
            default -> throw new IllegalArgumentException("Command not supported yet: " + cmd.name());
        }
    }

    private void blpop(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        var timeout = req.getLast();
        var resp = redisListCore.blpop(key, Double.parseDouble(timeout));
        if (resp == null) {
            RedisWriteProcessor.sendNull(outputStream);
        } else {
            RedisWriteProcessor.sendArray(outputStream, List.of(key, resp));
        }
    }

    private void lpop(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        if (req.size() == 2) {
            lpopSingleElement(key);
        } else {
            lpopMultipleElements(req, key);
        }
    }

    private void lpopMultipleElements(List<String> req, String key) throws IOException {
        var nPop = Integer.parseInt(req.get(2));
        var deletedList = redisListCore.lpop(key, nPop);
        RedisWriteProcessor.sendArray(outputStream, deletedList);
    }

    private void lpopSingleElement(String key) throws IOException {
        var resp = redisListCore.lpop(key);
        if (resp == null) {
            RedisWriteProcessor.sendNull(outputStream);
        } else {
            RedisWriteProcessor.sendBulkString(outputStream, resp);
        }
    }

    private void llen(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        var len = redisListCore.size(key);
        RedisWriteProcessor.sendInt(outputStream, len);
    }

    private void lrange(List<String> req) throws IOException {
        validateNumberOfArgs(req, 4);
        var key = req.get(1);
        var startIdx = Integer.parseInt(req.get(2));
        var endIdx = Integer.parseInt(req.get(3));
        var resp = redisListCore.lrange(key, startIdx, endIdx);
        RedisWriteProcessor.sendArray(outputStream, resp);
    }

    private void lpush(List<String> req) throws IOException {
        validateNumberOfArgs(req, 3);
        var key = req.get(1);
        var items = req.subList(2, req.size());
        var len = redisListCore.lpush(key, items);
        RedisWriteProcessor.sendInt(outputStream, len);
    }

    private void rpush(List<String> req) throws IOException {
        validateNumberOfArgs(req, 3);
        var key = req.get(1);
        var items = req.subList(2, req.size());
        var len = redisListCore.rpush(key, items);
        RedisWriteProcessor.sendInt(outputStream, len);
    }

    private void ping() throws IOException {
        RedisWriteProcessor.sendString(outputStream, "PONG");
    }

    private void get(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        var value = redisStringCore.get(key);
        if (value == null) {
            RedisWriteProcessor.sendNull(outputStream);
        } else {
            RedisWriteProcessor.sendBulkString(outputStream, value);
        }
    }

    private void set(List<String> req) throws IOException {
        validateNumberOfArgs(req, 3);
        var key = req.get(1);
        var value = req.get(2);
        int pxIdx = findStringIgnoreCase(req, "px", 3);
        if (pxIdx == -1) {
            redisStringCore.set(key, value);
        } else {
            redisStringCore.set(key, value, Long.parseLong(req.get(pxIdx + 1)));
        }
        RedisWriteProcessor.sendString(outputStream, "OK");
    }

    private void echo(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        RedisWriteProcessor.sendBulkString(outputStream, Objects.toString(req.get(1), ""));
    }

    private Protocol.Command getCmd(List<String> req) {
        validateNumberOfArgs(req, 1);
        var command = req.getFirst();
        return Optional.ofNullable(Protocol.Command.findCommand(command))
                .orElseThrow(() -> new IllegalArgumentException("Invalid command received: " + command));
    }

    public int findStringIgnoreCase(List<String> list, String str, int startIdx) {
        for (int i = startIdx; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(str)) {
                return i;
            }
        }
        return -1;
    }

    private void validateNumberOfArgs(List<String> req, int minSize) {
        if (req.size() < minSize) {
            throw new IllegalArgumentException("Wrong number of arguments for command");
        }
    }
}
