import server.nonblocking.NonBlockingServer;

public class Master {
    public static void main(String[] args) {
        var server = NonBlockingServer.init();
        server.startServer();
        server.stopServer();
    }
}
