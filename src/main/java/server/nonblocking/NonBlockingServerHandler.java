package server.nonblocking;

import redis.RedisHandler;
import redis.processor.RedisReadProcessor;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class NonBlockingServerHandler {
    static Conn handleKeyAccept(SocketChannel clientChannel) throws IOException {
        if (clientChannel == null) {
            return null;
        }

        // Get client address info
        InetSocketAddress clientAddr = (InetSocketAddress) clientChannel.getRemoteAddress();
        System.out.printf("New client from %s:%d%n", clientAddr.getAddress().getHostAddress(), clientAddr.getPort());

        clientChannel.configureBlocking(false);

        return new Conn(clientChannel);
    }

    static void handleWrite(Conn conn) {
        if (!conn.getWriter().hasRemaining()) {
            return;
        }
        try {
            var written = conn.getWriter().flush();

            if (written == 0) {
                return;
            }

            if (written < 0) {
                System.out.println("write() error - channel closed");
                conn.wantClose();
                return;
            }

            // update the readiness intention
            if (!conn.getWriter().hasRemaining()) {   // all data written
                conn.wantRead();
            }
        } catch (IOException e) {
            conn.wantClose();
            System.err.println("Write error: " + e.getMessage());
        }
    }

    static void handleRead(Conn conn) {
        try {
            var request = RedisReadProcessor.read(conn.getReader());
            if (request.isEmpty()) {
                return;
            }
            var redisHandler = new RedisHandler(conn.getWriter());
            redisHandler.handleCommand(request);

            // update the readiness intention
            if (conn.getWriter().hasRemaining()) {    // has a response
                conn.wantWrite();
                // The socket is likely ready to write in a request-response protocol,
                // try to write it without waiting for the next iteration.
                handleWrite(conn);
            }   // else: want read
        } catch (EOFException eof) {
            conn.wantClose(); // Client closed connection, do not log as error
        } catch (Exception e) { // Catch all other exceptions
            conn.wantClose();
            System.err.println("Read error: " + e.getMessage());
        }
    }

    static void updateSelectionKey(SelectionKey key, Conn conn) {
        if (conn.isWantClose()) {
            return; // Will be handled in the main loop
        }

        int ops = 0;
        if (conn.isWantRead()) ops |= SelectionKey.OP_READ;
        if (conn.isWantWrite()) ops |= SelectionKey.OP_WRITE;
        key.interestOps(ops);
    }
}
