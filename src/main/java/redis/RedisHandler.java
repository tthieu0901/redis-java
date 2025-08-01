package redis;

import protocol.Protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RedisHandler {
    private final RedisCore redisCore = RedisCore.getInstance();

    public void handleCommand(Socket socket, List<Object> request) throws IOException {
        var outputStream = socket.getOutputStream();
        var req = request.stream().filter(Objects::nonNull).map(Object::toString).toList();
        var cmd = getCmd(req);
        switch (cmd) {
            case PING -> handlePing(outputStream);
            case ECHO -> handleEcho(outputStream, req);
            case SET -> handleSet(outputStream, req);
            case GET -> handleGet(outputStream, req);
            default -> throw new IllegalArgumentException("Command not supported yet: " + cmd.name());
        }
    }

    private static Protocol.Command getCmd(List<String> req) {
        if (req.isEmpty()) {
            throw new IllegalArgumentException("No command received");
        }
        var command = req.getFirst();
        return Optional.ofNullable(Protocol.Command.findCommand(command))
                .orElseThrow(() -> new IllegalArgumentException("Invalid command received: " + command));
    }

    private static void handlePing(OutputStream outputStream) throws IOException {
        RedisWriteProcessor.sendString(outputStream, "PONG");
    }

    private void handleGet(OutputStream outputStream, List<String> req) throws IOException {
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

    private void handleSet(OutputStream outputStream, List<String> req) throws IOException {
        if (req.size() < 3) {
            throw new IllegalArgumentException("No key or value received");
        }
        var key = req.get(1);
        var value = req.get(2);
        redisCore.set(key, value);
        RedisWriteProcessor.sendString(outputStream, "OK");
    }

    private static void handleEcho(OutputStream outputStream, List<String> req) throws IOException {
        if (req.size() < 2) {
            throw new IllegalArgumentException("No message received");
        }
        RedisWriteProcessor.sendBulkString(outputStream, Objects.toString(req.get(1), ""));
    }

}
