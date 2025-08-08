package handler;

import error.ClientDisconnectException;
import error.NotEnoughDataException;
import redis.processor.RedisReadProcessor;
import redis.processor.RedisWriteProcessor;
import server.dto.Conn;
import server.info.ServerInfo;
import stream.Writer;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

public class ReplicaConnectHandler implements ConnHandler {
    private static final ReplicaConnectHandler INSTANCE = new ReplicaConnectHandler();

    private ReplicaConnectHandler() {
    }

    public static ReplicaConnectHandler getInstance() {
        return INSTANCE;
    }

    private boolean isPing = false;
    private boolean isFirstRepl = false;
    private boolean isSecondRepl = false;
    private boolean isPsync = false;

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
            checkPing(request);

            var writer = conn.getWriter();
            if (checkFirstRepl(writer, request)) return false;
            if (checkSecondRepl(writer, request)) return false;
            if (checkPsync(writer, request)) return false;

            System.out.println("Handshake OK");
        } catch (NotEnoughDataException e) {
            return false;
        }
        return true;
    }

    private void checkPing(List<Object> request) {
        if (isPing && !request.contains("PONG")) {
            handshakeFail();
        }
    }

    private boolean checkFirstRepl(Writer writer, List<Object> request) throws IOException {
        if (!isFirstRepl) {
            RedisWriteProcessor.sendArray(writer, List.of("REPLCONF", "listening-port", String.valueOf(ServerInfo.getInstance().getPort())));
            isFirstRepl = true;
            return true;
        }

        if (!request.contains("OK")) {
            handshakeFail();
        }
        return false;
    }

    private boolean checkSecondRepl(Writer writer, List<Object> request) throws IOException {
        if (!isSecondRepl) {
            RedisWriteProcessor.sendArray(writer, List.of("REPLCONF", "capa", "psync2"));
            isSecondRepl = true;
            return true;
        }

        if (!request.contains("OK")) {
            handshakeFail();
        }
        return false;
    }

    private boolean checkPsync(Writer writer, List<Object> ignored) throws IOException {
        if (!isPsync) {
            RedisWriteProcessor.sendArray(writer, List.of("PSYNC", "?", "-1"));
            isPsync = true;
            return true;
        }
        return false;
    }

    private void handshakeFail() {
        isPing = true;
        isFirstRepl = false;
        isSecondRepl = false;
        isPsync = false;
        throw new RuntimeException("Handshake failed");
    }
}
