package server.nonblocking;

import lombok.RequiredArgsConstructor;
import redis.RedisHandler;
import redis.processor.RedisReadProcessor;
import stream.BufferReader;
import stream.BufferWriter;

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

@RequiredArgsConstructor
public class NonBlockingServerHandler {
    private final Selector selector;
    private final ServerSocketChannel serverChannel;

    // Map to store connections by their SelectionKey
    private final Map<SelectionKey, Conn> connections = new HashMap<>();

    public void handleClient() throws IOException {
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
                handleKeyAccept();
                continue;
            }

            var conn = connections.get(key);
            if (conn == null) {
                continue;
            }

            // Handle readable connection
            if (key.isReadable()) {
                if (conn.isWantRead()) {
                    handleRead(conn);
                    updateSelectionKey(key, conn);
                }
            }
            // Handle writable connection
            else if (key.isWritable()) {
                if (conn.isWantWrite()) {
                    handleWrite(conn);
                    updateSelectionKey(key, conn);
                }
            }

            if (conn.isWantClose()) {
                key.cancel();
                conn.getChannel().close();
                connections.remove(key);
            }
        }
    }

    private void handleWrite(Conn conn) {
        if (conn.getOutgoing().dataSize() == 0) {
            return;
        }
        try {
            var writer = new BufferWriter(conn.getChannel());
            writer.flush();

            // update the readiness intention
            if (conn.getOutgoing().dataSize() == 0) {   // all data written
                conn.setWantRead(true);
                conn.setWantWrite(false);
            }
        } catch (IOException e) {
            conn.setWantClose(true);
            System.err.println("Write error: " + e.getMessage());
        }
    }

    private void handleRead(Conn conn) {
        try {
            var request = RedisReadProcessor.read(new BufferReader(conn.getChannel()));
            var redisHandler = new RedisHandler(new BufferWriter(conn.getChannel()));
            redisHandler.handleCommand(request);

            // update the readiness intention
            if (conn.getOutgoing().dataSize() > 0) {   // all data written
                conn.setWantRead(true);
                conn.setWantWrite(false);
                handleWrite(conn);
            }
        } catch (IOException e) {
            conn.setWantClose(true);
            System.err.println("Read error: " + e.getMessage());
        }
    }

    private void handleKeyAccept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) {
            return;
        }

        // Get client address info
        InetSocketAddress clientAddr = (InetSocketAddress) clientChannel.getRemoteAddress();
        System.out.printf("New client from %s:%d%n", clientAddr.getAddress().getHostAddress(), clientAddr.getPort());

        clientChannel.configureBlocking(false);

        var key = clientChannel.register(selector, SelectionKey.OP_READ);
        var conn = new Conn(clientChannel);
        connections.put(key, conn);
    }

    private void updateSelectionKey(SelectionKey key, Conn conn) {
        if (conn.isWantClose()) {
            return; // Will be handled in the main loop
        }

        int ops = 0;
        if (conn.isWantRead()) ops |= SelectionKey.OP_READ;
        if (conn.isWantWrite()) ops |= SelectionKey.OP_WRITE;
        key.interestOps(ops);
    }
}
