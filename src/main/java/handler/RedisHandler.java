package handler;

import error.ClientDisconnectException;
import error.ConnSleepException;
import error.NotEnoughDataException;
import redis.RedisCoreHandler;
import redis.processor.RedisReadProcessor;
import server.dto.Conn;

import java.io.EOFException;
import java.io.IOException;

public class RedisHandler implements IConnHandler {
    private static final RedisHandler INSTANCE = new RedisHandler();

    private RedisHandler() {
    }

    public static RedisHandler getInstance() {
        return INSTANCE;
    }


    @Override
    public void process(Conn conn) {
        try {
            // due to multi-pipelining, we muse loop here
            while (tryOneRequest(conn)) ;

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

    private boolean tryOneRequest(Conn conn) throws NotEnoughDataException, IOException {
        var reader = conn.getReader();
        reader.mark();
        try {
            var request = RedisReadProcessor.read(reader);
            var redisHandler = new RedisCoreHandler(conn);
            redisHandler.handleCommand(request);
        } catch (NotEnoughDataException e) {
            reader.reset();
            return false;
        } catch (ConnSleepException e) {
            conn.wantWrite();
        }
        reader.commit(); // Only when you handle successfully do you consume !!!
        return true;
    }
}
