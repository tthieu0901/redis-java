package unittest;

import client.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import extension.RedisServer;
import utils.TestHelper;

import java.util.List;

import static utils.ConstHelper.REDIS_HOSTNAME;
import static utils.ConstHelper.REDIS_PORT;

@ExtendWith(RedisServer.class)
class BasicRedisOperationTest {
    private Client client;

    @BeforeEach
    void beforeEach() {
        client = TestHelper.startClient(REDIS_HOSTNAME, REDIS_PORT);
    }

    @AfterEach
    void afterEach() {
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
}