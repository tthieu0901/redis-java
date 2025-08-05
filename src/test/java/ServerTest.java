import org.junit.jupiter.api.*;
import server.Server;
import server.nonblocking.NonBlockingServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    void beforeEach() {
        client = TestHelper.startClient(HOSTNAME, PORT);
    }

    @AfterEach
    void afterEach() {
        TestHelper.stopClient(client);
    }

    @AfterAll
    void tearDown() {
        stopServer();
    }

    private void startServer() throws InterruptedException {
        server = new NonBlockingServer(HOSTNAME, PORT);

        // Start server in background thread
        serverTask = CompletableFuture.runAsync(() -> {
            server.startServer();
        });

        // Give server a moment to start up
        Thread.sleep(500);
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
                System.out.println("Server shutdown exception: " + e.getMessage());
                // Force cancel if it's still running
                serverTask.cancel(true);
            }
        }
    }

    @Test
    void testServer_ping_pong() {
        var message = client.sendString("PING");
        TestHelper.expectSimpleString("PONG", message);
    }

    @Test
    void testServer_echo() {
        var message = client.sendArray(List.of("ECHO", "Hello, world"));
        TestHelper.expectBulkString("Hello, world", message);
    }

    @Test
    void testServer_set() {
        var message = client.sendArray(List.of("SET", "abc", "Hello, world"));
        TestHelper.expectSimpleString("OK", message);
    }

    @Test
    void testServer_setThenGet() {
        var setMessage = client.sendArray(List.of("SET", "test_set_then_get", "Hello, world"));
        TestHelper.expectSimpleString("OK", setMessage);

        var getMessage = client.sendArray(List.of("GET", "test_set_then_get"));
        TestHelper.expectBulkString("Hello, world", getMessage);
    }

    @Test
    void testServer_getNotFound() {
        var getMessage = client.sendArray(List.of("GET", "test_get_not_found"));
        TestHelper.expectNull(getMessage);
    }

    @Test
    void testServer_setWithExpiryTimeThenWaitAndGet() throws InterruptedException {
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
    void testServer_rpush() {
        var message = client.sendArray(List.of("RPUSH", "test_rpush", "Hello", "world"));
        TestHelper.expectInt(2, message);
    }

    @Test
    void testServer_rpushThenLrange() {
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
    void testServer_rpushThenLrange_withNegativeIdx() {
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
    void testServer_lpushThenLrange() {
        TestHelper.expectInt(3, client.sendArray(List.of("LPUSH", "test_lpush", "a", "b", "c")));

        var resp1 = client.sendArray(List.of("LRANGE", "test_lpush", "0", "-1"));
        TestHelper.expectArray(List.of("c", "b", "a"), resp1);
    }

    @Test
    void testServer_llen() {
        TestHelper.expectInt(3, client.sendArray(List.of("LPUSH", "test_llen", "a", "b", "c")));
        TestHelper.expectInt(3, client.sendArray(List.of("LLEN", "test_llen")));
    }

    @Test
    void testServer_lpop() {
        TestHelper.expectInt(5, client.sendArray(List.of("RPUSH", "test_lpop", "a", "b", "c", "d", "e")));
        TestHelper.expectBulkString("a", client.sendArray(List.of("LPOP", "test_lpop")));
        TestHelper.expectArray(List.of("b", "c"), client.sendArray(List.of("LPOP", "test_lpop", "2")));
        TestHelper.expectArray(List.of("d", "e"), client.sendArray(List.of("LRANGE", "test_lpop", "0", "-1")));

        TestHelper.expectArray(List.of("d", "e"), client.sendArray(List.of("LPOP", "test_lpop", "2")));
        TestHelper.expectArray(List.of(), client.sendArray(List.of("LRANGE", "test_lpop", "0", "-1")));

        TestHelper.expectNull(client.sendArray(List.of("LPOP", "test_lpop")));
    }

    @Disabled
    @Test
    void testServer_blpop() throws ExecutionException, InterruptedException {
        var taskClient2 = TestHelper.runOnAnotherClient(
                HOSTNAME, PORT,
                c -> c.sendArray(List.of("BLPOP", "test_blpop", "0")
                ));


        var taskClient3 = TestHelper.runOnAnotherClient(
                HOSTNAME, PORT,
                c -> c.sendArray(List.of("BLPOP", "test_blpop", "0")
                ));

        Thread.sleep(10); // wait for the request from client 3 to come

        TestHelper.expectInt(1, client.sendArray(List.of("RPUSH", "test_blpop", "a")));
        TestHelper.expectArray(List.of("test_blpop", "a"), taskClient2.get());

        TestHelper.expectInt(0, client.sendArray(List.of("LLEN", "test_blpop")));


        Thread.sleep(200); // wait for the second push

        TestHelper.expectInt(1, client.sendArray(List.of("RPUSH", "test_blpop", "b")));
        TestHelper.expectArray(List.of("test_blpop", "b"), taskClient3.get());
    }

    @Disabled
    @Test
    void testServer_blpopWithTimeout() throws ExecutionException, InterruptedException {
        TestHelper.expectNull(client.sendArray(List.of("BLPOP", "test_blpop_timeout", "0.1")));

        var taskClient2 = TestHelper.runOnAnotherClient(
                HOSTNAME, PORT,
                c -> c.sendArray(List.of("BLPOP", "test_blpop_timeout", "0.5")
                ));

        Thread.sleep(10); // wait for the request

        TestHelper.expectInt(1, client.sendArray(List.of("RPUSH", "test_blpop_timeout", "a")));
        TestHelper.expectArray(List.of("test_blpop_timeout", "a"), taskClient2.get());
    }
}