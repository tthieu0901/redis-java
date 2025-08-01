package redis;

import protocol.Protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RedisHandler {
    private final RedisCore redisCore = RedisCore.getInstance();
    private final OutputStream outputStream;

    public RedisHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void handleCommand(List<Object> request) throws IOException {
        var req = request.stream().filter(Objects::nonNull).map(Object::toString).toList();
        var cmd = getCmd(req);
        switch (cmd) {
            case PING -> handlePing();
            case ECHO -> handleEcho(req);
            case SET -> handleSet(req);
            case GET -> handleGet(req);
            default -> throw new IllegalArgumentException("Command not supported yet: " + cmd.name());
        }
    }

    private Protocol.Command getCmd(List<String> req) {
        if (req.isEmpty()) {
            throw new IllegalArgumentException("No command received");
        }
        var command = req.getFirst();
        return Optional.ofNullable(Protocol.Command.findCommand(command))
                .orElseThrow(() -> new IllegalArgumentException("Invalid command received: " + command));
    }

    private void handlePing() throws IOException {
        RedisWriteProcessor.sendString(outputStream, "PONG");
    }

    private void handleGet(List<String> req) throws IOException {
        if (req.size() < 2) {
            throw new IllegalArgumentException("No key received");
        }
        var key = req.get(1);
        var value = redisCore.get(key);
        if (value == null) {
            RedisWriteProcessor.sendNull(outputStream);
        } else {
            RedisWriteProcessor.sendBulkString(outputStream, value);
        }
    }

    private void handleSet(List<String> req) throws IOException {
        if (req.size() < 3) {
            throw new IllegalArgumentException("No key or value received");
        }
        var key = req.get(1);
        var value = req.get(2);
        int pxIdx = findStringIgnoreCase(req, "px", 3);
        if (pxIdx == -1) {
            redisCore.set(key, value);
        } else {
            redisCore.set(key, value, Long.parseLong(req.get(pxIdx + 1)));
        }
        RedisWriteProcessor.sendString(outputStream, "OK");
    }

    private void handleEcho(List<String> req) throws IOException {
        if (req.size() < 2) {
            throw new IllegalArgumentException("No message received");
        }
        RedisWriteProcessor.sendBulkString(outputStream, Objects.toString(req.get(1), ""));
    }

    public int findStringIgnoreCase(List<String> list, String str, int startIdx) {
        for (int i = startIdx; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(str)) {
                return i;
            }
        }
        return -1;
    }
}
