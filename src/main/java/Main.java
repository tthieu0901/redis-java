import server.Server;
import server.nonblocking.NonBlockingServer;

public class Main {
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_HOSTNAME = "localhost";

    public static void main(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        Server server = new NonBlockingServer(DEFAULT_HOSTNAME, port);
        server.startServer();
        server.stopServer();
    }
}
