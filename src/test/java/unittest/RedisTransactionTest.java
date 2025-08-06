package unittest;

import client.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.RedisServer;
import utils.TestHelper;

import java.util.List;

import static utils.ConstHelper.REDIS_HOSTNAME;
import static utils.ConstHelper.REDIS_PORT;

@ExtendWith(RedisServer.class)
public class RedisTransactionTest {
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
    void incr_keyExists_incrValue() {
        TestHelper.expectSimpleString("OK", client.sendArray(List.of("SET", "foo", "5")));
        TestHelper.expectInt(6, client.sendArray(List.of("INCR", "foo")));
    }

    @Test
    void incr_keyNotExists_setMinusOne() {
        TestHelper.expectInt(1, client.sendArray(List.of("INCR", "not_exist")));
    }
}
