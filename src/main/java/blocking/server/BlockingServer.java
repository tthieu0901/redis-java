package blocking.server;

import server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class is for handling the multi-thread approach. This is not the way Redis handled, but I gave it a try.
 * <p>
 * To mimic Redis, I refactored the code and used Event Loop, but this class should work just fine! (even though, multi-thread is indeed hard to control)
 */
@Deprecated
public class BlockingServer implements Server {
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private ExecutorService executorService;
    private BlockingServerHandler blockingServerHandler;
    private final int port;

    public BlockingServer(int port) {
        this.port = port;
    }

    @Override
    public void startServer() {
        executorService = Executors.newCachedThreadPool(); // Better thread management
        blockingServerHandler = new BlockingServerHandler();

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000); // Add timeout to accept() calls

            running = true;
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> blockingServerHandler.handleClientSocket(this, clientSocket));
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

    boolean isRunning() {
        return this.running;
    }
}
