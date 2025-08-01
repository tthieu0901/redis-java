import org.junit.jupiter.api.*;
import server.Server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTest {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 6379;

    private Server server;
    private Client client;
    private CompletableFuture<Void> serverTask;

    @BeforeAll
    void setUp() throws Exception {
        startServer();
    }

    @BeforeEach
    void beforeEach() throws IOException {
        startClient();
    }

    private void startServer() throws InterruptedException {
        server = new Server();

        // Start server in background thread
        serverTask = CompletableFuture.runAsync(() -> {
            server.startServer();
        });

        // Give server a moment to start up
        Thread.sleep(500);
    }

    private void startClient() throws IOException {
        client = new Client();
        client.connect(HOSTNAME, PORT);
    }

    @AfterEach
    void afterEach() throws IOException {
        client.disconnect();
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
    void testServer_Ping() throws IOException, InterruptedException {
        var message = client.sendString("PING");
        var expected = "+PONG\r\n";
        Assertions.assertEquals("+PONG\r\n", message.substring(0, expected.length()));
    }

    @Test
    void testServer_Echo() throws IOException, InterruptedException {
        var message = client.sendArray(List.of("ECHO", "Hello, world"));
        var expected = "$12\r\nHello, world\r\n";
        Assertions.assertEquals("$12\r\nHello, world\r\n", message.substring(0, expected.length()));
    }
}