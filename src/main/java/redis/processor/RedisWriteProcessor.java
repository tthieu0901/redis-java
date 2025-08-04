package redis.processor;

import protocol.Protocol;
import stream.Writer;

import java.io.IOException;
import java.util.List;

public class RedisWriteProcessor {
    protected static final String CRLF = "\r\n";
    private static final String NULL_VALUE = "$-1";

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
}
