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

    enum AckCommand {
        PING,
        FIRST_REPL,
        SECOND_REPL,
        PSYNC,
        ;
    }

    static final class AckOperation {
        public AckCommand getAckCommand() {
            return ackCommand;
        }

        private final AckCommand ackCommand;
        private boolean isSent;
        private boolean isReceived;

        AckOperation(AckCommand ackCommand, boolean isSent, boolean isReceived) {
            this.ackCommand = ackCommand;
            this.isSent = isSent;
            this.isReceived = isReceived;
        }

        AckOperation(AckCommand ackCommand) {
            this(ackCommand, false, false);
        }
    }

    private final List<AckOperation> ackProcess = List.of(
            new AckOperation(AckCommand.PING),
            new AckOperation(AckCommand.FIRST_REPL),
            new AckOperation(AckCommand.SECOND_REPL),
            new AckOperation(AckCommand.PSYNC)
    );

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
            ackProcess.getFirst().isSent = true;
            var request = RedisReadProcessor.read(conn.getReader());
            verifyCurrentAck(request);

            var writer = conn.getWriter();
            sendNewAckOperation(writer);

            if (ackProcess.getLast().isReceived) {
                System.out.println("Handshake OK");
            }

        } catch (NotEnoughDataException e) {
            return false;
        } catch (IOException e) {
            handshakeFail();
            throw e;
        }
        return true;
    }

    private void sendNewAckOperation(Writer writer) throws IOException {
        AckOperation currentOperation = null;
        for (AckOperation process : ackProcess) {
            if (!process.isSent) {
                currentOperation = process;
                break;
            }
        }

        if (currentOperation != null) {
            currentOperation.isSent = true;
            switch (currentOperation.getAckCommand()) {
                case FIRST_REPL ->
                        RedisWriteProcessor.sendArray(writer, List.of("REPLCONF", "listening-port", String.valueOf(ServerInfo.getInstance().getPort())));
                case SECOND_REPL -> RedisWriteProcessor.sendArray(writer, List.of("REPLCONF", "capa", "psync2"));
                case PSYNC -> RedisWriteProcessor.sendArray(writer, List.of("PSYNC", "?", "-1"));
            }
        }
    }

    private void verifyCurrentAck(List<Object> request) {
        AckOperation currentOperation = null;
        for (AckOperation ack : ackProcess) {
            if (!ack.isReceived) {
                currentOperation = ack;
                break;
            }
        }

        if (currentOperation == null) {
            return;
        }

        var isCorrectAckResponse = switch (currentOperation.getAckCommand()) {
            case PING -> request.contains("PONG");
            case FIRST_REPL, SECOND_REPL -> request.contains("OK");
            case PSYNC -> !request.isEmpty() && request.getFirst().toString().contains("FULLRESYNC");
        };

        if (isCorrectAckResponse) {
            currentOperation.isReceived = true;
        } else {
            handshakeFail();
        }
    }

    private void handshakeFail() {
        for (var ack : ackProcess) {
            ack.isSent = false;
            ack.isReceived = false;
        }
        ackProcess.getFirst().isSent = true;
        throw new RuntimeException("Handshake failed");
    }
}
