package server;

import protocol.Protocol;
import redis.RedisCore;
import redis.RedisReadProcessor;
import redis.RedisWriteProcessor;
import stream.RedisInputStream;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Server {
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final RedisCore redisCore = RedisCore.getInstance();

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
            while (running && !socket.isClosed()) {
                var inputStream = new RedisInputStream(socket.getInputStream());
                if (inputStream.available() > 0) {
                    List<Object> resp = RedisReadProcessor.read(inputStream);
                    handleCommand(socket, resp);
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

    private void handleCommand(Socket socket, List<Object> resp) throws IOException {
        var outputStream = socket.getOutputStream();

        if (resp.isEmpty()) {
            throw new IllegalArgumentException("No command received");
        }
        var command = resp.getFirst();
        if (command instanceof String) {
            System.out.println("Received command: " + command);
        } else {
            System.out.println("Received invalid command: " + command);
            throw new IllegalArgumentException("Invalid command received: " + command);
        }
        var cmd = Protocol.Command.findCommand(Objects.toString(command));
        if (cmd == null) {
            throw new IllegalArgumentException("Invalid command received: " + command);
        }
        switch (cmd) {
            case PING -> RedisWriteProcessor.sendString(outputStream, "PONG");
            case ECHO -> {
                if (resp.size() < 2) {
                    throw new IllegalArgumentException("No message received");
                }
                RedisWriteProcessor.sendBulkString(outputStream, Objects.toString(resp.get(1), ""));
            }
            case SET -> {
                if (resp.size() < 3) {
                    throw new IllegalArgumentException("No key or value received");
                }
                var key = Objects.toString(resp.get(1), "");
                var value = Objects.toString(resp.get(2), "");
                redisCore.set(key, value);
                RedisWriteProcessor.sendString(outputStream, "OK");
            }
            case GET -> {
                if (resp.size() < 2) {
                    throw new IllegalArgumentException("No key received");
                }
                var key = Objects.toString(resp.get(1), "");
                var value = redisCore.get(key);
                if (value == null) {
                    RedisWriteProcessor.sendNull(outputStream);
                } else {
                    RedisWriteProcessor.sendBulkString(outputStream, value);
                }
            }
            default -> throw new IllegalArgumentException("Command not supported yet: " + command);
        }
    }

}
