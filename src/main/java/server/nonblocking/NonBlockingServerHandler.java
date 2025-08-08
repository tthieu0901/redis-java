package server.nonblocking;

import handler.ConnHandler;
import redis.processor.RedisWriteProcessor;
import server.dto.Conn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NonBlockingServerHandler {
    public static Conn handleKeyAccept(SocketChannel clientChannel) throws IOException {
        if (clientChannel == null) {
            return null;
        }

        // Get client address info
        InetSocketAddress clientAddr = (InetSocketAddress) clientChannel.getRemoteAddress();
        System.out.printf("New client from %s:%d%n", clientAddr.getAddress().getHostAddress(), clientAddr.getPort());

        clientChannel.configureBlocking(false);

        return new Conn(clientChannel, Conn.ConnectionType.CLIENT_CONNECT);
    }

    public static void handleWrite(Conn conn) {
        if (!conn.getWriter().hasRemaining()) {
            return;
        }
        try {
            var written = conn.getWriter().flush();

            if (written == 0) {
                return;
            }

            if (written < 0) {
                System.err.println("write() error - channel closed");
                conn.wantClose();
                return;
            }

            if (!conn.getWriter().hasRemaining()) {
                conn.wantRead();
            }
        } catch (IOException e) {
            System.err.println("Write error: " + e.getMessage());
            conn.wantClose();
        }
    }

    public static void handleRead(Conn conn, ConnHandler task) {
        task.process(conn);

        if (conn.getWriter().hasRemaining()) {
            conn.wantWrite();
            // The socket is likely ready to write in a request-response protocol,
            // try to write it without waiting for the next iteration.
            handleWrite(conn);
        }
    }

    public static void updateSelectionKey(SelectionKey key, Conn conn) {
        if (conn.isWantClose()) {
            return; // Will be handled in the main loop
        }

        int ops = 0;
        if (conn.isWantRead()) ops |= SelectionKey.OP_READ;
        if (conn.isWantWrite()) ops |= SelectionKey.OP_WRITE;
        key.interestOps(ops);
    }

    public static void handleMasterConnect(Conn conn) throws IOException {
        RedisWriteProcessor.sendString(conn.getWriter(), "PING");

        if (conn.getWriter().hasRemaining()) {
            conn.wantWrite();
            // The socket is likely ready to write in a request-response protocol,
            // try to write it without waiting for the next iteration.
            handleWrite(conn);
        }
    }
}

