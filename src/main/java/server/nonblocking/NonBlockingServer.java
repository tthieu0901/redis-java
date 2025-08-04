package server.nonblocking;

import server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class NonBlockingServer implements Server {
    private final int port;
    private final String hostName;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running = true;


    public NonBlockingServer(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    @Override
    public void startServer() {
        try {
            // Create selector
            selector = Selector.open();

            // Create server socket channel
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            // Bind to address
            serverChannel.bind(new InetSocketAddress(hostName, port));

            // Register server channel with selector
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Redis server listening on port " + port);

            var nonBlockServerHandler =  new NonBlockingServerHandler(selector, serverChannel);
            while (running) {
                nonBlockServerHandler.handleClient();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    @Override
    public void stopServer() {
        running = false; // Signal server loop to stop

        // Interrupt the selector thread if it's blocked on select()
        selector.wakeup();

        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close(); // Stop accepting new connections
            }

            if (selector != null && selector.isOpen()) {
                // Close all client channels
                Set<SelectionKey> keys = selector.keys();
                for (SelectionKey key : keys) {
                    if (key.channel() instanceof SocketChannel) {
                        try {
                            key.channel().close();
                        } catch (IOException e) {
                            System.err.println("Error closing client channel: " + e.getMessage());
                        }
                    }
                }
                selector.close(); // Close the selector
            }
        } catch (IOException e) {
            System.err.println("Error during server shutdown: " + e.getMessage());
        }
        System.out.println("NIO server stopped.");
    }
}
