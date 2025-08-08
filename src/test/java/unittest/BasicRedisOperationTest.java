package unittest;

import client.Client;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import extension.RedisServer;
import utils.TestHelper;

import java.util.List;

import static utils.ConstHelper.REDIS_HOSTNAME;
import static utils.ConstHelper.REDIS_PORT;

@ExtendWith(RedisServer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BasicRedisOperationTest {
    private Client client;

    @BeforeAll
    void beforeAll() {
        client = TestHelper.startClient(REDIS_HOSTNAME, REDIS_PORT);
    }

    @AfterAll
    void afterAll() {
        TestHelper.stopClient(client);
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
        TestHelper.expectOk(message);
    }

    @Test
    void testServer_setThenGet() {
        var setMessage = client.sendArray(List.of("SET", "test_set_then_get", "Hello, world"));
        TestHelper.expectOk(setMessage);

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
        var setMessage = client.sendArray(List.of("SET", "test_set_with_expiry_time", "Hello, world", "pX", "200"));
        TestHelper.expectOk(setMessage);

        var getMessage = client.sendArray(List.of("GET", "test_set_with_expiry_time"));
        TestHelper.expectBulkString("Hello, world", getMessage);

        // Wait for expiry time
        Thread.sleep(400);

        var expiredMessage = client.sendArray(List.of("GET", "test_set_with_expiry_time"));
        TestHelper.expectNull(expiredMessage);
    }

    @Test
    void info_replication_returnReplicationInfo() {
        TestHelper.expectBulkString("role:master", client.sendArray(List.of("INFO", "replication")));
    }
}