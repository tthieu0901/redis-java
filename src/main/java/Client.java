import utils.RedisReadProcessor;
import utils.RedisWriteProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class Client {
    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.sendString("PING");
    }

    public List<Object> sendString(String message) {
        try (Socket socket = new Socket()) {
            // Add connection timeout of 5 seconds
            socket.connect(new InetSocketAddress("localhost", 6379), 5000);
            socket.setSoTimeout(5000); // Add read timeout
            System.out.println("Connected to server");

            var resp = handle(message, socket);

            System.out.println("Disconnected from server");
            return resp;
        } catch (IOException e) {
            System.out.println("Could not connect to server");
            return null;
        }
    }

//    public String sendList(List<String> messages) {
//        try (Socket socket = new Socket()) {
//            // Add connection timeout of 5 seconds
//            socket.connect(new InetSocketAddress("localhost", 6379), 5000);
//            socket.setSoTimeout(5000); // Add read timeout
//            System.out.println("Connected to server");
//
//            var resp = handle(message, socket);
//
//            System.out.println("Disconnected from server");
//            return resp;
//        } catch (IOException e) {
//            System.out.println("Could not connect to server");
//            return null;
//        }
//    }

    private static List<Object> handle(String message, Socket socket) throws IOException {
        RedisWriteProcessor.sendString(socket, message);
        return RedisReadProcessor.process(socket.getInputStream());
    }

}
