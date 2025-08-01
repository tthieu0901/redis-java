package utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class RedisWriteProcessor extends RedisProcessor {
    public static void sendString(Socket socket, String message) throws IOException {
        sendMessage(socket, String.valueOf(message.length()));
        sendMessage(socket, message);
    }

    public static void sendMessage(Socket socket, String message) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write((message + CRLF).getBytes());
    }
}
