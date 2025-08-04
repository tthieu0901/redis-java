import server.Server;
import server.nonblocking.NonBlockingServer;

public class Main {
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_HOSTNAME = "localhost";

    public static void main(String[] args) {
        Server server = new NonBlockingServer(DEFAULT_HOSTNAME, DEFAULT_PORT);
        server.startServer();
        server.stopServer();
    }
}
