package server.nonblocking;

import handler.IConnHandler;
import server.Server;
import server.cron.ReplicateDataCron;
import server.cron.TimeoutCron;
import server.dto.Conn;
import server.info.ServerInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class NonBlockingServer implements Server {
    private final int port;
    private final String hostName;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running = true;

    private boolean isMasterDown = true;
    private boolean isRetryConnectToMaster = false;

    // Map to store connections by their SelectionKey
    private final Map<SelectionKey, Conn> connections = new HashMap<>();

    public NonBlockingServer(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public static Server init() {
        return init(new String[0]);
    }

    public static Server init(String[] args) {
        var serverInfo = ServerInfo.getInstance();
        serverInfo.init(args);
        return new NonBlockingServer(serverInfo.getHostName(), serverInfo.getPort());
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

            var crons = List.of(ReplicateDataCron.getInstance(),  TimeoutCron.getInstance());

            while (running) {
                retryConnectToMaster();
                for (var cron: crons) {
                    cron.run();
                }

                int channels = selector.select(100);

                // Only skip I/O processing if no channels are ready,
                // but always continue to process cron jobs
                if (channels == 0) {
                    continue;
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

                    // Handle connectable connection
                    if (key.isConnectable()) {
                        try {
                            var channel = conn.getChannel();
                            if (channel.finishConnect()) {
                                channel.configureBlocking(false);
                                isMasterDown = false;
                                System.out.println("Connected to master - " +
                                        ServerInfo.getInstance().getMasterHostName() + ":" +
                                        ServerInfo.getInstance().getMasterPort());

                                // Send initial handshake/commands to master if needed
                                // This might be needed depending on your protocol
                                NonBlockingServerHandler.handleMasterConnect(conn);

                                // Update selection key to reflect current connection state
                                NonBlockingServerHandler.updateSelectionKey(key, conn);
                            }
                        } catch (IOException e) {
                            System.err.println("Disconnected from master");
                            isMasterDown = true;
                            conn.wantClose();
                        }
                    }
                    // Handle readable connection
                    else if (key.isReadable() && conn.isWantRead()) {
                        NonBlockingServerHandler.handleRead(conn, IConnHandler::handle);
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

                        if (conn.getConnectionType().equals(Conn.ConnectionType.REPLICA_CONNECT)) {
                            isMasterDown = true;
                        }

                        ReplicateDataCron.getInstance().removeReplica(conn);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void retryConnectToMaster() {
        if (!ServerInfo.getInstance().isReplica() || !isMasterDown) {
            return;
        }

        // Only register a new retry if we're not already retrying
        if (isRetryConnectToMaster) {
            return;
        }

        // Mark that we're starting a retry attempt
        isRetryConnectToMaster = true;

        TimeoutCron.getInstance().registerTimeout(2000, () -> {
            if (!isMasterDown) {
                isRetryConnectToMaster = false;
                return; // Stop retry - master is back up
            }

            String masterHostName = ServerInfo.getInstance().getMasterHostName();
            int masterPort = ServerInfo.getInstance().getMasterPort();
            System.out.println("Retry to connect to master - " + masterHostName + ":" + masterPort);

            try {
                var socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                var masterAddress = new InetSocketAddress(masterHostName, masterPort);

                socketChannel.connect(masterAddress);

                SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);

                Conn conn = new Conn(socketChannel, Conn.ConnectionType.REPLICA_CONNECT);
                connections.put(key, conn);
            } catch (IOException e) {
                // Connection failed, continue retrying
                System.err.println("Failed to connect to master - " + masterHostName + ":" + masterPort);
            }

            isRetryConnectToMaster = false;
        });
    }

    @Override
    public void stopServer() {
        running = false; // Signal server loop to stop
        isMasterDown = false;
        isRetryConnectToMaster = true;

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
