package unittest;

import client.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import extension.RedisServer;
import utils.TestHelper;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static utils.ConstHelper.REDIS_HOSTNAME;
import static utils.ConstHelper.REDIS_PORT;

@ExtendWith(RedisServer.class)
class RedisListOperationTest {
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

    @Test
    void testServer_blpop() throws ExecutionException, InterruptedException {
        var taskClient2 = TestHelper.startClientAndSendRequest(
                c -> c.sendArray(List.of("BLPOP", "test_blpop", "0")));

        var taskClient3 = TestHelper.startClientAndSendRequest(
                c -> c.sendArray(List.of("BLPOP", "test_blpop", "0")));

        TestHelper.expectInt(1, client.sendArray(List.of("RPUSH", "test_blpop", "a")));
        TestHelper.expectArray(List.of("test_blpop", "a"), taskClient2.get());

        TestHelper.expectInt(0, client.sendArray(List.of("LLEN", "test_blpop")));

        TestHelper.expectInt(1, client.sendArray(List.of("RPUSH", "test_blpop", "b")));
        TestHelper.expectArray(List.of("test_blpop", "b"), taskClient3.get());
    }

    @Test
    void testServer_blpopWithTimeout() throws ExecutionException, InterruptedException {
        TestHelper.expectNull(client.sendArray(List.of("BLPOP", "test_blpop_timeout", "0.1")));

        var taskClient2 = TestHelper.startClientAndSendRequest(
                c -> c.sendArray(List.of("BLPOP", "test_blpop_timeout", "0.5")));

        TestHelper.expectInt(1, client.sendArray(List.of("RPUSH", "test_blpop_timeout", "a")));
        TestHelper.expectArray(List.of("test_blpop_timeout", "a"), taskClient2.get());
    }

    @Test
    void testServer_blpopWithTimeout_andDoTimeout() throws ExecutionException, InterruptedException {
        var taskClient2 = TestHelper.startClientAndSendRequest(
                c -> c.sendArray(List.of("BLPOP", "test_blpop_timeout", "0.5")));

        Thread.sleep(600);

        TestHelper.expectNull(taskClient2.get());
    }
}