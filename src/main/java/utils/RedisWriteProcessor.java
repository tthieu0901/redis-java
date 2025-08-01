package utils;

import protocol.Protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class RedisWriteProcessor extends RedisProcessor {
    public static void sendString(OutputStream outputStream, String message) throws IOException {
        sendMessage(outputStream, Protocol.DataType.SIMPLE_STRING.getPrefix() + message);
    }

    public static void sendBulkString(OutputStream outputStream, String message) throws IOException {
        sendMessage(outputStream, Protocol.DataType.BULK_STRING.getPrefix() + String.valueOf(message.length()));
        sendMessage(outputStream, message);
    }

    public static void sendArray(OutputStream outputStream, List<String> messages) throws IOException {
        sendMessage(outputStream, Protocol.DataType.ARRAY.getPrefix() + String.valueOf(messages.size()));
        for (String message : messages) {
            sendBulkString(outputStream, message);
        }
    }

    public static void sendMessage(OutputStream outputStream, String message) throws IOException {
        outputStream.write((message + CRLF).getBytes());
        outputStream.flush();
    }
}
