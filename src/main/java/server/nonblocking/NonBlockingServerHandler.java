package server.nonblocking;

import error.ClientDisconnectException;
import error.NotEnoughDataException;
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

    static void handleRead(Conn conn) {
        try {
            // due to multi-pipelining, we muse loop here
            while (tryOneRequest(conn)) {

            }

            if (conn.getWriter().hasRemaining()) {
                conn.wantWrite();
                // The socket is likely ready to write in a request-response protocol,
                // try to write it without waiting for the next iteration.
                handleWrite(conn);
            }
        } catch (ClientDisconnectException eof) {
            System.out.println("Client disconnected");
            conn.wantClose();
        } catch (EOFException eof) {
            System.err.println("Client disconnected due to unexpected EOF - " + eof.getMessage());
            conn.wantClose();
        } catch (Exception e) { // Catch all other exceptions
            System.err.println("Read error: " + e.getMessage());
            conn.wantClose();
        }
    }

    private static boolean tryOneRequest(Conn conn) throws IOException {
        var reader = conn.getReader();
        reader.mark();
        try {
            var request = RedisReadProcessor.read(reader);
            var redisHandler = new RedisHandler(conn.getWriter());
            redisHandler.handleCommand(request);
        } catch (NotEnoughDataException e) {
            reader.reset();
            return false;
        }
        reader.commit(); // Only when you handle successfully do you consume !!!
        return true;
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
