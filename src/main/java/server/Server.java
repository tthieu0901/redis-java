package server;

import redis.RedisHandler;
import redis.RedisReadProcessor;
import stream.RedisInputStream;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private ExecutorService executorService;

    public void startServer() {
        int port = 6379;
        executorService = Executors.newCachedThreadPool(); // Better thread management

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000); // Add timeout to accept() calls

            running = true;
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleClientSocket(clientSocket));
                } catch (SocketTimeoutException e) {
                    // Normal timeout, continue loop
                    continue;
                } catch (IOException e) {
                    if (!running) {
                        break;
                    }
                    throw e;
                }
            }
        } catch (Exception e) {
            if (running) { // Only log if not shutting down
                System.out.println("Server exception: " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            if (executorService != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } catch (Exception e) {
            System.out.println("Error stopping server: " + e.getMessage());
        }
    }


    private void handleClientSocket(Socket socket) {
        try {
            System.out.println("Client connected: " + socket.getRemoteSocketAddress());
            socket.setSoTimeout(5000); // Set read timeout

            RedisHandler handler = new RedisHandler(socket.getOutputStream());
            var inputStream = new RedisInputStream(socket.getInputStream());

            while (running && !socket.isClosed() && socket.isConnected()) {
                try {
                    if (inputStream.available() > 0) {
                        List<Object> req = RedisReadProcessor.read(inputStream);
                        handler.handleCommand(req);
                    } else {
                        Thread.sleep(50); // Increased sleep time
                    }
                } catch (IOException e) {
                    // Client disconnected or timeout
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Client " + socket.getRemoteSocketAddress() + " error: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
            System.out.println("Client handler finished for " + socket.getRemoteSocketAddress());
        }
    }

}
