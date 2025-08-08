import server.nonblocking.NonBlockingServer;

public class Replica {
    public static void main(String[] args) {
        var server = NonBlockingServer.init(new String[]{"--port", "6380", "--replicaof", "localhost 6379"});
        server.startServer();
        server.stopServer();
    }
}
