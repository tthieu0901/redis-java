import stream.RedisInputStream;
import utils.RedisWriteProcessor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class Client {
    private Socket socket;
    private RedisInputStream inputStream;
    private OutputStream outputStream;

    public void connect(String hostName, int port) throws IOException {
        socket = new Socket();
        // Add connection timeout of 5 seconds
        socket.connect(new InetSocketAddress(hostName, port), 5000);
        socket.setSoTimeout(5000); // Add read timeout
        System.out.println("Connected to server");

        outputStream = socket.getOutputStream();
        inputStream = new RedisInputStream(socket.getInputStream());

    }

    public void disconnect() throws IOException {
        outputStream.close();
        inputStream.close();
        if (socket != null) socket.close();
        System.out.println("Disconnected from server");
    }

    public String sendString(String message) throws IOException, InterruptedException {
        RedisWriteProcessor.sendString(outputStream, message);
        inputStream.waitTillAvailable(5000);
        return inputStream.readAll();
    }

    public String sendArray(List<String> messages) throws IOException, InterruptedException {
        RedisWriteProcessor.sendArray(outputStream, messages);
        inputStream.waitTillAvailable(5000);
        return inputStream.readAll();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client();
        client.connect("localhost", 6379);
        var message = client.sendArray(List.of("ECHO", "Hello, world"));
        System.out.println("Received message: " + message);
        client.disconnect();
    }
}