package unittest;

import client.Client;
import extension.RedisServerExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.TestHelper;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static utils.ConstHelper.REDIS_HOSTNAME;
import static utils.ConstHelper.REDIS_PORT;

@ExtendWith(RedisServerExtension.class)
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
    void exec_execNoMulti_returnError() {
        TestHelper.expectError("EXEC without MULTI", client.sendArray(List.of("EXEC")));
    }

    @Test
    void exec_emptyTransaction_returnEmptyTransaction() {
        TestHelper.expectOk(client.sendArray(List.of("MULTI")));
        TestHelper.expectArray(List.of(), client.sendArray(List.of("EXEC")));
        TestHelper.expectError("EXEC without MULTI", client.sendArray(List.of("EXEC")));
    }

    @Test
    void exec_transaction_shouldQueueTransaction() throws ExecutionException, InterruptedException {
        TestHelper.expectOk(client.sendArray(List.of("MULTI")));
        TestHelper.expectQueued(client.sendArray(List.of("SET", "test_queue", "12")));
        TestHelper.expectQueued(client.sendArray(List.of("INCR", "test_queue")));

        var newClient = TestHelper.startClientAndSendRequest(c -> c.sendArray(List.of("GET", "test_queue")));
        TestHelper.expectNull(newClient.get());

        client.sendArray(List.of("EXEC"));
        TestHelper.expectBulkString("13", client.sendArray(List.of("GET", "test_queue")));
    }

    @Test
    void exec_discardNoMulti_shouldNotExecCommands() {
        TestHelper.expectError("DISCARD without MULTI", client.sendArray(List.of("DISCARD")));
    }

    @Test
    void exec_discardTransaction_shouldNotExecCommands() {
        TestHelper.expectOk(client.sendArray(List.of("MULTI")));
        TestHelper.expectQueued(client.sendArray(List.of("SET", "test_discard", "12")));
        TestHelper.expectQueued(client.sendArray(List.of("INCR", "test_discard")));
        TestHelper.expectOk(client.sendArray(List.of("DISCARD")));
        TestHelper.expectNull(client.sendArray(List.of("GET", "test_discard")));
    }
}
