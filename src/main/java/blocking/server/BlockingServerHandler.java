package blocking.server;

import blocking.redis.internal.BlockingRedisHandler;
import redis.processor.RedisReadProcessor;
import stream.RedisInputStream;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

@Deprecated
class BlockingServerHandler {

    void handleClientSocket(BlockingServer server, Socket socket) {
        try {
            System.out.println("Client connected: " + socket.getRemoteSocketAddress());
            socket.setSoTimeout(5000); // Set read timeout

            var handler = new BlockingRedisHandler(socket.getOutputStream());
            var inputStream = new RedisInputStream(socket.getInputStream());

            while (server.isRunning() && !socket.isClosed() && socket.isConnected()) {
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
            System.out.println("Client " + socket.getRemoteSocketAddress() + " error: " + e.fillInStackTrace());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

}
