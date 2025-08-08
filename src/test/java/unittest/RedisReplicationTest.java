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
        if (redisServer != null) {
            redisServer.stopServer();
        }
        if (client != null) {
            TestHelper.stopClient(client);
        }
    }

    @Test
    void info_master_returnMasterInfo() throws InterruptedException {
        redisServer = RedisServer.init(new String[]{"--port", "6380"});
        redisServer.startServer();

        client = TestHelper.startClient(RedisServer.DEFAULT_HOSTNAME, 6380);
        var message = client.sendArray(List.of("INFO", "replication"));
        Assertions.assertTrue(message.contains("role:master"));
        Assertions.assertTrue(message.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"));
        Assertions.assertTrue(message.contains("master_repl_offset:0"));
    }

    @Test
    void info_replica_returnReplicaInfo() throws InterruptedException {
        redisServer = RedisServer.init(new String[]{"--port", "6380", "--replicaof", "\"localhost 6379\""});
        redisServer.startServer();

        client = TestHelper.startClient(RedisServer.DEFAULT_HOSTNAME, 6380);
        var message = client.sendArray(List.of("INFO", "replication"));
        Assertions.assertTrue(message.contains("role:slave"));
    }
}
