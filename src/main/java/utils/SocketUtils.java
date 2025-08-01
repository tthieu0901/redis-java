package utils;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SocketUtils {
    private static final String CRLF = "\r\n";

    public static String readMessage(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        Scanner scanner = new Scanner(new InputStreamReader(inputStream));
        scanner.useDelimiter(CRLF);
        return scanner.nextLine();
    }

    public static String read(Socket socket) throws IOException {
        var resp = readMessage(socket);

        return sanitizeData(resp);
    }

    public static String sanitizeData(String data) {
        return data.replaceAll("\r\n", "");
    }

    public static void sendString(Socket socket, String message) throws IOException {
        sendMessage(socket, String.valueOf(message.length()));
        sendMessage(socket, message);
    }

    public static void sendMessage(Socket socket, String message) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write((message + CRLF).getBytes());
    }
}
