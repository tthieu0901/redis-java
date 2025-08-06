package blocking.server;

import blocking.redis.internal.BlockingRedisHandler;
import redis.processor.RedisReadProcessor;
import stream.RedisInputStream;
import stream.RedisOutputStream;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

@Deprecated
class BlockingServerHandler {

    void handleClientSocket(BlockingServer server, Socket socket) {
        try {
            System.out.println("client.Client connected: " + socket.getRemoteSocketAddress());
            socket.setSoTimeout(5000); // Set read timeout

            var outputStream = new RedisOutputStream(socket.getOutputStream());
            var inputStream = new RedisInputStream(socket.getInputStream());

            var handler = new BlockingRedisHandler(outputStream);

            while (server.isRunning() && !socket.isClosed() && socket.isConnected()) {
                try {
                    if (inputStream.available() > 0) {
                        List<Object> req = RedisReadProcessor.read(inputStream);
                        handler.handleCommand(req);
                        outputStream.flush();
                    } else {
                        Thread.sleep(50); // Increased sleep time
                    }
                } catch (IOException e) {
                    // client.Client disconnected or timeout
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("client.Client " + socket.getRemoteSocketAddress() + " error: " + e.fillInStackTrace());
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
