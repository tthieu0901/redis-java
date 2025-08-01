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
    void tearDown() {
        stopServer();
    }

    private void stopServer() {
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
    void testServer_ping_pong() throws IOException {
        var message = client.sendString("PING");
        assertEquals("+PONG\r\n", message);
        TestHelper.expectSimpleString("PONG", message);
    }

    @Test
    void testServer_echo() throws IOException {
        var message = client.sendArray(List.of("ECHO", "Hello, world"));
        TestHelper.expectBulkString("Hello, world", message);
    }

    @Test
    void testServer_set() throws IOException {
        var setMessage = client.sendArray(List.of("SET", "abc", "Hello, world"));
        assertEquals("+OK\r\n", setMessage);
    }

    @Test
    void testServer_setThenGet() throws IOException {
        var setMessage = client.sendArray(List.of("SET", "test_set_then_get", "Hello, world"));
        TestHelper.expectSimpleString("OK", setMessage);

        var getMessage = client.sendArray(List.of("GET", "test_set_then_get"));
        TestHelper.expectBulkString("Hello, world", getMessage);
    }

    @Test
    void testServer_getNotFound() throws IOException {
        var getMessage = client.sendArray(List.of("GET", "test_get_not_found"));
        TestHelper.expectNull(getMessage);
    }

    @Test
    void testServer_setWithExpiryTimeThenWaitAndGet() throws IOException, InterruptedException {
        var setMessage = client.sendArray(List.of("SET", "test_set_with_expiry_time", "Hello, world", "pX", "500"));
        TestHelper.expectSimpleString("OK", setMessage);

        var getMessage = client.sendArray(List.of("GET", "test_set_with_expiry_time"));
        TestHelper.expectBulkString("Hello, world", getMessage);

        // Wait for expiry time
        Thread.sleep(700);

        var expiredMessage = client.sendArray(List.of("GET", "test_set_with_expiry_time"));
        TestHelper.expectNull(expiredMessage);
    }

    @Test
    void testServer_rpush() throws IOException {
        var message = client.sendArray(List.of("RPUSH", "test_rpush", "Hello", "world"));
        TestHelper.expectInt(2, message);
    }

    @Test
    void testServer_rpushThenLrange() throws IOException {
        var message = client.sendArray(List.of("RPUSH", "test_lrange", "a", "b", "c"));
        TestHelper.expectInt(3, message);

        var resp1 = client.sendArray(List.of("LRANGE", "test_lrange", "0", "1"));
        TestHelper.expectArray(List.of("a", "b"), resp1);

        var resp2 = client.sendArray(List.of("LRANGE", "test_lrange", "1", "4"));
        TestHelper.expectArray(List.of("b", "c"), resp2);

        var resp3 = client.sendArray(List.of("LRANGE", "test_lrange", "3", "2"));
        TestHelper.expectArray(List.of(), resp3);

        var resp4 = client.sendArray(List.of("LRANGE", "test_lrange_not_found", "3", "2"));
        TestHelper.expectArray(List.of(), resp4);
    }

    @Test
    void testServer_rpushThenLrange_withNegativeIdx() throws IOException {
        var message = client.sendArray(List.of("RPUSH", "test_lrange_neg", "a", "b", "c", "d", "e"));
        TestHelper.expectInt(5, message);

        var resp1 = client.sendArray(List.of("LRANGE", "test_lrange_neg", "-2", "-1"));
        TestHelper.expectArray(List.of("d", "e"), resp1);

        var resp2 = client.sendArray(List.of("LRANGE", "test_lrange_neg", "0", "-3"));
        TestHelper.expectArray(List.of("a", "b", "c"), resp2);

        var resp3 = client.sendArray(List.of("LRANGE", "test_lrange_neg", "2", "-1"));
        TestHelper.expectArray(List.of("c", "d", "e"), resp3);
    }

    @Test
    void testServer_lpushThenLrange() throws IOException {
        TestHelper.expectInt(3, client.sendArray(List.of("LPUSH", "test_lpush", "a", "b", "c")));

        var resp1 = client.sendArray(List.of("LRANGE", "test_lpush", "0", "-1"));
        TestHelper.expectArray(List.of("c", "b", "a"), resp1);
    }

    @Test
    void testServer_llen() throws IOException {
        TestHelper.expectInt(3, client.sendArray(List.of("LPUSH", "test_llen", "a", "b", "c")));
        TestHelper.expectInt(3, client.sendArray(List.of("LLEN", "test_llen")));
    }
}