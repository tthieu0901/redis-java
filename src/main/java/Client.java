import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379));
            System.out.println("Connected to server");
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write("PING\nPING\nPING\n".getBytes());

            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            while (bytesRead != -1) {
                System.out.println(new String(buffer, 0, bytesRead));
                bytesRead = inputStream.read(buffer);
            }
            System.out.println("Disconnected from server");
            inputStream.close();
            outputStream.close();
            System.out.println("Closing socket");
        } catch (IOException e) {
            System.out.println("Could not connect to server");
            throw e;
        }
    }
}
