package handler;

import server.dto.Conn;

public interface IConnHandler {
    void process(Conn conn);

    static void handle(Conn conn) {
        var handler = switch (conn.getConnectionType()) {
            case CLIENT_CONNECT -> RedisHandler.getInstance();
            case REPLICA_CONNECT ->  ReplicaConnectHandler.getInstance();
        };
        handler.process(conn);
    }
}
