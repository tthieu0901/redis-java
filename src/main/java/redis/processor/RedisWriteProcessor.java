package redis.processor;

import protocol.Protocol;
import redis.Response;
import stream.Writer;

import java.io.IOException;
import java.util.List;

public class RedisWriteProcessor {
    protected static final String CRLF = "\r\n";
    private static final String NULL_VALUE = "$-1";

    public static void sendError(Writer writer, String errorMessage) throws IOException {
        sendMessage(writer, String.format("%sERR %s", Protocol.DataType.ERROR.getPrefix(), errorMessage));
    }

    public static void sendNull(Writer writer) throws IOException {
        sendMessage(writer, NULL_VALUE);
    }

    public static void sendString(Writer writer, String message) throws IOException {
        sendMessage(writer, Protocol.DataType.SIMPLE_STRING.getPrefix() + message);
    }

    public static void sendInt(Writer writer, int message) throws IOException {
        sendMessage(writer, Protocol.DataType.INTEGER.getPrefix() + String.valueOf(message));
    }

    public static void sendBulkString(Writer writer, String message) throws IOException {
        sendMessage(writer, Protocol.DataType.BULK_STRING.getPrefix() + String.valueOf(message.length()));
        sendMessage(writer, message);
    }

    public static void sendArray(Writer writer, List<String> messages) throws IOException {
        sendMessage(writer, Protocol.DataType.ARRAY.getPrefix() + String.valueOf(messages.size()));
        for (String message : messages) {
            sendBulkString(writer, message);
        }
    }

    public static void sendMessage(Writer writer, String message) throws IOException {
        writer.write(message + CRLF);
    }

    public static void sendResponse(Writer writer, Response response) throws IOException {
        switch (response.responseType()) {
            case STRING -> RedisWriteProcessor.sendString(writer, (String) response.message());
            case ERROR -> RedisWriteProcessor.sendError(writer, (String) response.message());
            case INTEGER -> RedisWriteProcessor.sendInt(writer, (int) response.message());
            case BULK_STRING -> RedisWriteProcessor.sendBulkString(writer, (String) response.message());
            // TODO: Handle unchecked cast later
            case ARRAY_STRING -> RedisWriteProcessor.sendArray(writer, (List<String>) response.message());
            case ARRAY_RESPONSE -> {
                var responses = (List<Response>) response.message();
                RedisWriteProcessor.sendMessage(writer, Protocol.DataType.ARRAY.getPrefix() + String.valueOf(responses.size()));
                for (var resp : responses) {
                    sendResponse(writer, resp);
                }
            }
            case NULL -> RedisWriteProcessor.sendNull(writer);
        }
    }
}
