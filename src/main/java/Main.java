import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            // Wait for connection from client.
            clientSocket = serverSocket.accept();

            handleClientSocket(clientSocket);

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private static void handleClientSocket(Socket clientSocket) throws IOException {
        String messages = readMessage(clientSocket);
        var nPing = count(messages, "PING");
        for (int i = 0; i < nPing; i++) {
            sendMessage(clientSocket, "+PONG\r\n");
        }
    }

    private static String readMessage(Socket socket) {
        InputStream inputStream = null;
        StringBuilder message = new StringBuilder();
        try {
            inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            message.append(new String(buffer, 0, bytesRead));
            return message.toString();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
        return "";
    }

    private static void sendMessage(Socket socket, String message) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(message.getBytes());
    }

    public static int count(String text, String find) {
        int index = 0, count = 0, length = find.length();
        while ((index = text.indexOf(find, index)) != -1) {
            index += length;
            count++;
        }
        return count;
    }
}
