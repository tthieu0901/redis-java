package server.nonblocking;

import server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;

public class NonBlockingServer implements Server {
    private ServerSocketChannel serverSocket;
    private final int port;
    private boolean running = true;

    public NonBlockingServer(int port) {
        this.port = port;
    }

    @Override
    public void startServer() {
        try {
            serverSocket = new ServerSocketChannel();
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000); // Add timeout to accept() calls

            running = true;
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
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

    @Override
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

    boolean isRunning() {
        return this.running;
    }
}
