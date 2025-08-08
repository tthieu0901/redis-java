package unittest;

import client.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.RedisServer;
import utils.TestHelper;

import java.io.IOException;
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
    void info_master_returnMaster() throws InterruptedException, IOException {
        redisServer = RedisServer.init(new String[]{"--port", "6380"});
        redisServer.startServer();

        client = TestHelper.startClient(RedisServer.DEFAULT_HOSTNAME, 6380);
        TestHelper.expectBulkString("role:master", client.sendArray(List.of("INFO", "replication")));
    }

    @Test
    void info_replica_returnReplica() throws InterruptedException, IOException {
        redisServer = RedisServer.init(new String[]{"--port", "6380", "--replicaof", "\"localhost 6379\""});
        redisServer.startServer();

        client = TestHelper.startClient(RedisServer.DEFAULT_HOSTNAME, 6380);
        TestHelper.expectBulkString("role:slave", client.sendArray(List.of("INFO", "replication")));
    }
}
