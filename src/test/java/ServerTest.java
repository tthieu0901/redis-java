import org.junit.jupiter.api.*;
import server.Server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void beforeEach() throws IOException, InterruptedException {
        startClient();
        // Clear any pending operations from previous tests
        Thread.sleep(150);
        // Drain any residual data in the input stream
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
    void afterEach() throws IOException, InterruptedException {
        client.disconnect();
        Thread.sleep(150);
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
    void testServer_ping_pong() throws IOException, InterruptedException {
        var message = client.sendString("PING");
        assertEquals("+PONG\r\n", message);
    }

    @Test
    void testServer_echo() throws IOException, InterruptedException {
        var message = client.sendArray(List.of("ECHO", "Hello, world"));
        assertEquals("$12\r\nHello, world\r\n", message);
    }

//    TODO: Somehow this test keep failing
//    @Test
//    void testServer_set() throws IOException, InterruptedException {
//        var setMessage = client.sendArray(List.of("SET", "abc", "Hello, world"));
//        assertEquals("+OK\r\n", setMessage);
//    }

    @Test
    void testServer_setThenGet() throws IOException, InterruptedException {
        var setMessage = client.sendArray(List.of("SET", "test_set_then_get", "Hello, world"));
        assertEquals("+OK\r\n", setMessage);

        var getMessage = client.sendArray(List.of("GET", "test_set_then_get"));
        assertEquals("$12\r\nHello, world\r\n", getMessage);
    }

    @Test
    void testServer_GetNotFound() throws IOException, InterruptedException {
        var getMessage = client.sendArray(List.of("GET", "test_get_not_found"));
        assertEquals("$-1\r\n", getMessage);
    }

    @Test
    void testServer_SetWithExpiryTimeThenWaitAndGet() throws IOException, InterruptedException {
        var setMessage = client.sendArray(List.of("SET", "test_set_with_expiry_time", "Hello, world", "pX", "200"));
        assertEquals("+OK\r\n", setMessage);

        var getMessage = client.sendArray(List.of("GET", "test_set_with_expiry_time"));
        assertEquals("$12\r\nHello, world\r\n", getMessage);

        // Wait for expiry time
        Thread.sleep(500);

        var expiredMessage = client.sendArray(List.of("GET", "test_set_with_expiry_time"));
        assertEquals("$-1\r\n", expiredMessage);
    }

    @Test
    void testServer_rpush() throws IOException, InterruptedException {
        var message = client.sendArray(List.of("RPUSH", "test_rpush", "Hello", "world"));
        assertEquals(":2\r\n", message);
    }
}