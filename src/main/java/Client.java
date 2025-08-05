import redis.processor.RedisWriteProcessor;
import stream.Reader;
import stream.RedisInputStream;
import stream.RedisOutputStream;
import stream.Writer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class Client {
    private static final int TIMEOUT = 50000;
    private Socket socket;
    private Reader inputStream;
    private Writer outputStream;

    public void connect(String hostName, int port) throws IOException {
        socket = new Socket();
        // Add connection timeout of 5 seconds
        socket.connect(new InetSocketAddress(hostName, port), TIMEOUT);
        socket.setSoTimeout(TIMEOUT); // Add read timeout
        System.out.println("Connected to server");

        outputStream = new RedisOutputStream(socket.getOutputStream());
        inputStream = new RedisInputStream(socket.getInputStream());

    }

    public void disconnect() throws IOException {
        outputStream.close();
        inputStream.close();
        if (socket != null) socket.close();
        System.out.println("Disconnected from server");
    }

    public String sendString(String message) {
        try {
            RedisWriteProcessor.sendString(outputStream, message);
            outputStream.flush();
            return inputStream.readAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendArray(List<String> messages) {
        try {
            RedisWriteProcessor.sendArray(outputStream, messages);
            outputStream.flush();
            return inputStream.readAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.connect("localhost", 6379);
         var message = client.sendArray(List.of("ECHO", "Hello, world"));
//        var message = client.sendArray(List.of("RPUSH", "test_rpush", "Hello", "world"));
        System.out.println("Received message: " + message);
        client.disconnect();
    }
}