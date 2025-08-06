package redis;

import error.ConnSleepException;
import protocol.Protocol;
import redis.internal.NonBlockingRedisListCore;
import redis.internal.NonBlockingRedisStringCore;
import redis.internal.RedisListCore;
import redis.processor.RedisWriteProcessor;
import server.cron.ServerCron;
import stream.Writer;

import java.io.IOException;
import java.util.*;

public class RedisHandler {
    private static final String INVALID_INTEGER = "value is not an integer or out of range";
    private final NonBlockingRedisStringCore redisStringCore;
    private final RedisListCore redisListCore;
    private final Writer writer;
    private static final HashMap<String, Queue<Request>> REQUEST_QUEUE = new HashMap<>();

    public RedisHandler(Writer writer) {
        this.writer = writer;
        this.redisStringCore = NonBlockingRedisStringCore.getInstance();
        this.redisListCore = NonBlockingRedisListCore.getInstance();
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
            case INCR  -> incr(req);
            default -> throw new IllegalArgumentException("Command not supported yet: " + cmd.name());
        }
    }

    private void incr(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        try {
            var resp = redisStringCore.incr(key); // TODO: what if key is timeout ??
            RedisWriteProcessor.sendInt(writer, Integer.parseInt(resp));
        } catch (NumberFormatException e) {
            RedisWriteProcessor.sendError(writer, INVALID_INTEGER);
        }
    }

    private void blpop(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        var timeoutSeconds = req.getLast();

        final Request request;
        if ("0".equals(timeoutSeconds)) { // Wait indefinitely
            request = new Request(writer);
        } else {
            var timeoutMillis = (long) (Double.parseDouble(timeoutSeconds) * 1000L);
            request = new Request(writer, timeoutMillis);
        }

        var resp = redisListCore.lpop(key);
        if (resp != null) {
            RedisWriteProcessor.sendArray(writer, List.of(key, resp));
        } else {
            var queue = REQUEST_QUEUE.computeIfAbsent(key, _ -> new ArrayDeque<>());
            queue.add(request);

            // register to serverCron to handle client timeout
            ServerCron.getInstance().registerTimeout(request.getTtlMillis(), () -> {
                try {
                    if (REQUEST_QUEUE.get(key).contains(request)) {
                        RedisWriteProcessor.sendNull(writer);
                        REQUEST_QUEUE.get(key).remove(request);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            throw new ConnSleepException();
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
        RedisWriteProcessor.sendArray(writer, deletedList);
    }

    private void lpopSingleElement(String key) throws IOException {
        var resp = redisListCore.lpop(key);
        if (resp == null) {
            RedisWriteProcessor.sendNull(writer);
        } else {
            RedisWriteProcessor.sendBulkString(writer, resp);
        }
    }

    private void llen(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        var len = redisListCore.size(key);
        RedisWriteProcessor.sendInt(writer, len);
    }

    private void lrange(List<String> req) throws IOException {
        validateNumberOfArgs(req, 4);
        var key = req.get(1);
        var startIdx = Integer.parseInt(req.get(2));
        var endIdx = Integer.parseInt(req.get(3));
        var resp = redisListCore.lrange(key, startIdx, endIdx);
        RedisWriteProcessor.sendArray(writer, resp);
    }

    private void lpush(List<String> req) throws IOException {
        validateNumberOfArgs(req, 3);
        var key = req.get(1);
        var items = req.subList(2, req.size());
        var len = redisListCore.lpush(key, items);
        RedisWriteProcessor.sendInt(writer, len);
        tryBlop(key);
    }

    private void rpush(List<String> req) throws IOException {
        validateNumberOfArgs(req, 3);
        var key = req.get(1);
        var items = req.subList(2, req.size());
        var len = redisListCore.rpush(key, items);
        RedisWriteProcessor.sendInt(writer, len);
        tryBlop(key);
    }

    private void ping() throws IOException {
        RedisWriteProcessor.sendString(writer, "PONG");
    }

    private void get(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        var key = req.get(1);
        var value = redisStringCore.get(key);
        if (value == null) {
            RedisWriteProcessor.sendNull(writer);
        } else {
            RedisWriteProcessor.sendBulkString(writer, value);
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
        RedisWriteProcessor.sendString(writer, "OK");
    }

    private void echo(List<String> req) throws IOException {
        validateNumberOfArgs(req, 2);
        RedisWriteProcessor.sendBulkString(writer, Objects.toString(req.get(1), ""));
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

    private void tryBlop(String key) throws IOException {
        var queue = REQUEST_QUEUE.get(key);
        if (queue == null) {
            return;
        }

        while (!queue.isEmpty()) {
            var request = queue.peek();
            var writer = request.getWriter();

            if (request.isTimeout()) {
                RedisWriteProcessor.sendNull(writer);
                queue.poll();
                continue;
            }

            var value = redisListCore.lpop(key);
            if (value == null) {
                break;
            }

            RedisWriteProcessor.sendArray(writer, List.of(key, value));
            queue.poll();
        }
    }
}
