package server.nonblocking;

import server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NonBlockingServer implements Server {
    private final int port;
    private final String hostName;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running = true;

    // Map to store connections by their SelectionKey
    private final Map<SelectionKey, Conn> connections = new HashMap<>();

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

            while (running) {
                int channels = selector.select();
                if (channels == 0) {
                    return;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        var conn = NonBlockingServerHandler.handleKeyAccept(clientChannel);
                        if (conn != null) {
                            var clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
                            connections.put(clientKey, conn);
                        }
                        continue;
                    }

                    var conn = connections.get(key);
                    if (conn == null) {
                        continue;
                    }

                    // Handle readable connection
                    if (key.isReadable() && conn.isWantRead()) {
                        NonBlockingServerHandler.handleRead(conn);
                        NonBlockingServerHandler.updateSelectionKey(key, conn);
                    }
                    // Handle writable connection
                    else if (key.isWritable() && conn.isWantWrite()) {
                        NonBlockingServerHandler.handleWrite(conn);
                        NonBlockingServerHandler.updateSelectionKey(key, conn);
                    }

                    if (conn.isWantClose()) {
                        key.cancel();
                        conn.getChannel().close();
                        connections.remove(key);
                    }
                }
            }
        } catch (
                IOException e) {
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
