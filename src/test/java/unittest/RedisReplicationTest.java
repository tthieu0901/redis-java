package unittest;

import client.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import server.RedisServer;
import utils.TestHelper;

import java.util.List;

class RedisReplicationTest {
    private RedisServer redisServer;
    private Client client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            TestHelper.stopClient(client);
        }
        if (redisServer != null) {
            redisServer.stopServer();
        }
    }

    @Test
    void info_master_returnMasterInfo() throws InterruptedException {
        redisServer = RedisServer.init(new String[]{"--port", "5432"});
        redisServer.startServer();

        client = TestHelper.startClient(RedisServer.DEFAULT_HOSTNAME, 5432);
        var message = client.sendArray(List.of("INFO", "replication"));
        Assertions.assertTrue(message.contains("role:master"));
        Assertions.assertTrue(message.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"));
        Assertions.assertTrue(message.contains("master_repl_offset:0"));
    }

    @Test
    void info_replica_returnReplicaInfo() throws InterruptedException {
        redisServer = RedisServer.init(new String[]{"--port", "1234", "--replicaof", "localhost 4321"});
        redisServer.startServer();

        client = TestHelper.startClient(RedisServer.DEFAULT_HOSTNAME, 1234);
        var message = client.sendArray(List.of("INFO", "replication"));
        Assertions.assertTrue(message.contains("role:slave"));
    }
}
