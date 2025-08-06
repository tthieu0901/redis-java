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
class RedisTransactionTest {
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
        TestHelper.expectOk(client.sendArray(List.of("SET", "test_exist", "5")));
        TestHelper.expectInt(6, client.sendArray(List.of("INCR", "test_exist")));
    }

    @Test
    void incr_keyNotExists_setDefaultValue() {
        TestHelper.expectInt(1, client.sendArray(List.of("INCR", "test_not_exist")));
    }

    @Test
    void incr_keyCannotParse_returnError() {
        TestHelper.expectOk(client.sendArray(List.of("SET", "test_cannot_parse", "Hello World!")));
        TestHelper.expectError("value is not an integer or out of range", client.sendArray(List.of("INCR", "test_cannot_parse")));
    }

    @Test
    void multi_returnOk() {
        TestHelper.expectOk(client.sendArray(List.of("MULTI")));
    }

    @Test
    void exec_noMulti_returnError() {
        TestHelper.expectError("EXEC without MULTI", client.sendArray(List.of("EXEC")));
    }
}
