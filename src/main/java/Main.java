import server.Server;
import server.nonblocking.NonBlockingServer;

public class Main {

    public static void main(String[] args) {
        Server server = NonBlockingServer.init(args);
        server.startServer();
        server.stopServer();
    }
}
