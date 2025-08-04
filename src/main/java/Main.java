import server.Server;
import server.nonblocking.NonBlockingServer;

public class Main {
    private static final int DEFAULT_PORT = 6379;

    public static void main(String[] args) {
        Server server = new NonBlockingServer(DEFAULT_PORT);
        server.startServer();
        server.stopServer();
    }
}
