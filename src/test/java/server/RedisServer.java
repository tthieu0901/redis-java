package server;

import server.nonblocking.NonBlockingServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RedisServer {
    public static final String DEFAULT_PORT = "6379";
    public static final String DEFAULT_HOSTNAME = "localhost";

    private CompletableFuture<Void> serverTask;
    private final Server server;

    private RedisServer(String[] args) {
        server = NonBlockingServer.init(args);
    }

    public static RedisServer init() {
        return new RedisServer(new String[]{});
    }

    public static RedisServer init(String[] args) {
        return new RedisServer(args);
    }

    public void startServer() throws InterruptedException {
        var latch = new CountDownLatch(1);
        // Start server in background thread
        serverTask = CompletableFuture.runAsync(() -> {
            latch.countDown();
            server.startServer();
        });

        latch.await();

        Thread.sleep(500); // Wait for sometime for server to start up
    }

    public void stopServer() {
        if (server != null) {
            server.stopServer();
        }

        // Wait for server task to complete or timeout
        if (serverTask != null) {
            try {
                // The task might throw an exception on shutdown, which is expected
                serverTask.exceptionally(_ -> null).get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("Server shutdown exception: " + e.getMessage());
                // Force cancel if it's still running
                serverTask.cancel(true);
            }
        }
    }
}
