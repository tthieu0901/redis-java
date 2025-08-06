package redis.internal;

import error.ExecNoMultiException;

import java.util.ArrayDeque;
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

    private static final Queue<Object> queue = new ArrayDeque<>();

    public void multi() {
        queue.offer(new Object());
    }

    public void exec() {
        if (queue.isEmpty()) {
            throw new ExecNoMultiException();
        }
        queue.poll();
    }
}
