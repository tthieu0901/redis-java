package handler;

import error.ClientDisconnectException;
import error.NotEnoughDataException;
import redis.processor.RedisReadProcessor;
import server.dto.Conn;

import java.io.EOFException;
import java.io.IOException;
import java.util.stream.Collectors;

public class ReplicaConnectHandler implements ConnHandler {
    private static final ReplicaConnectHandler INSTANCE = new ReplicaConnectHandler();

    private ReplicaConnectHandler() {
    }

    public static ReplicaConnectHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(Conn conn) {
        try {
            // due to multi-pipelining, we muse loop here
            while (ack(conn)) ;

        } catch (ClientDisconnectException eof) {
            System.out.println("Master disconnected");
            conn.wantClose();
        } catch (EOFException eof) {
            System.err.println("Master disconnected due to unexpected EOF - " + eof.getMessage());
            conn.wantClose();
        } catch (Exception e) { // Catch all other exceptions
            System.err.println("Read error: " + e.getMessage());
            conn.wantClose();
        }
    }

    private boolean ack(Conn conn) throws IOException {
        try {
            var request = RedisReadProcessor.read(conn.getReader());
            System.out.println("Received request from master: " + request.stream().map(Object::toString).collect(Collectors.joining(" ")));
        } catch (NotEnoughDataException e) {
            return false;
        }
        return true;
    }
}
