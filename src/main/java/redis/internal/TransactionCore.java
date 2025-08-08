package redis.internal;

import error.ExecNoMultiException;
import redis.Command;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

public class TransactionCore {
    // Singleton
    // ------------------------------------------------------------------
    private static final TransactionCore INSTANCE = new TransactionCore();

    private TransactionCore() {
    }

    public static TransactionCore getInstance() {
        return INSTANCE;
    }
    // ------------------------------------------------------------------

    private static final HashMap<String, Queue<Command>> TRANSACTION_QUEUE = new HashMap<>();

    public void multi(Command command) {
        TRANSACTION_QUEUE.putIfAbsent(command.getConnectionId(), new ArrayDeque<>());
    }

    public Queue<Command> exec(Command command) {
        var queue = TRANSACTION_QUEUE.get(command.getConnectionId());
        if (queue == null) {
            throw new ExecNoMultiException();
        }
        TRANSACTION_QUEUE.remove(command.getConnectionId());
        return queue;
    }
}
