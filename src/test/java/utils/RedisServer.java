package utils;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import server.Server;
import server.nonblocking.NonBlockingServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RedisServer implements BeforeAllCallback, AfterAllCallback, Extension {
    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final int DEFAULT_PORT = 6379;

    private CompletableFuture<Void> serverTask;
    private Server server;

    @Override
    public void afterAll(ExtensionContext context) {
        stopServer();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws InterruptedException {
        startServer();
    }

    private void startServer() throws InterruptedException {
        server = new NonBlockingServer(DEFAULT_HOSTNAME, DEFAULT_PORT);

        var latch = new CountDownLatch(1);
        // Start server in background thread
        serverTask = CompletableFuture.runAsync(() -> {
            latch.countDown();
            server.startServer();
        });

        latch.await();

        Thread.sleep(500); // Wait for sometime for server to start up
    }

    private void stopServer() {
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
