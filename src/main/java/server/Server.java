package server;

import redis.RedisHandler;
import redis.RedisReadProcessor;
import stream.RedisInputStream;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Server {
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public void startServer() {
        int port = 6379;
        try {
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            running = true;
            while (running && !serverSocket.isClosed()) {
                try {
                    // Wait for connection from client.
                    Socket clientSocket = serverSocket.accept();
                    CompletableFuture.runAsync(() -> handleClientSocket(clientSocket));
                } catch (IOException e) {
                    if (!running) {
                        break; // Exit the loop if server is stopping
                    }
                    throw e;
                }
            }
        } catch (Exception e) {
            System.out.println("server.Server exception: " + e.getMessage());
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.out.println("Error stopping server: " + e.getMessage());
        }
    }

    private void handleClientSocket(Socket socket) {
        try {
            System.out.println("Client connected: " + socket.getRemoteSocketAddress());
            RedisHandler handler = new RedisHandler(socket.getOutputStream());
            while (running && !socket.isClosed()) {
                var inputStream = new RedisInputStream(socket.getInputStream());
                if (inputStream.available() > 0) {
                    List<Object> req = RedisReadProcessor.read(inputStream);
                    handler.handleCommand(req);
                } else {
                    Thread.sleep(10);
                }
            }
        } catch (Exception e) {
            System.out.println("Client " + socket.getRemoteSocketAddress() + " disconnected");
            System.out.println("Client exception: " + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                System.out.println("Client exception: " + e.getMessage());
            }
        }
    }
}
