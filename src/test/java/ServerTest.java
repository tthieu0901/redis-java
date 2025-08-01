import org.junit.jupiter.api.*;
import server.Server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTest {
    private Server server;
    private CompletableFuture<Void> serverTask;

    @BeforeAll
    void setUp() throws Exception {
        server = new Server();

        // Start server in background thread
        serverTask = CompletableFuture.runAsync(() -> {
            server.startServer();
        });

        // Give server a moment to start up
        Thread.sleep(500);
    }

    @AfterAll
    void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }

        // Wait for server task to complete or timeout
        if (serverTask != null) {
            try {
                // The task might throw an exception on shutdown, which is expected
                serverTask.exceptionally(ex -> null).get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("server.Server shutdown exception: " + e.getMessage());
                // Force cancel if it's still running
                serverTask.cancel(true);
            }
        }
    }

    @Test
    void testServerEcho() {
        Client client = new Client();
        var message = client.sendString("PING");
        Assertions.assertEquals("PING\r\n", message);
    }
}