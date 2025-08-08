package server.cron;

import java.util.PriorityQueue;
import java.util.function.Supplier;

public class RetryCron {
    private static final RetryCron INSTANCE = new RetryCron();

    private RetryCron() {
    }

    public static RetryCron getInstance() {
        return INSTANCE;
    }

    private final PriorityQueue<RetryEvent> retryQueue = new PriorityQueue<>();

    public void tick() {
        long now = System.currentTimeMillis();
        while (!retryQueue.isEmpty() && retryQueue.peek().nextRunTimeMillis <= now) {
            RetryEvent event = retryQueue.poll();
            boolean done = event.task.get();  // If true, stop retrying
            if (!done) {
                event.scheduleNext();          // Reschedule
                retryQueue.add(event);
            }
        }
    }

    public void registerRetry(long intervalMillis, Supplier<Boolean> task) {
        retryQueue.add(new RetryEvent(intervalMillis, task));
    }

    private static class RetryEvent implements Comparable<RetryEvent> {
        final long intervalMillis;
        final Supplier<Boolean> task;
        long nextRunTimeMillis;

        RetryEvent(long intervalMillis, Supplier<Boolean> task) {
            this.intervalMillis = intervalMillis;
            this.task = task;
            this.nextRunTimeMillis = System.currentTimeMillis();
        }

        void scheduleNext() {
            this.nextRunTimeMillis = System.currentTimeMillis() + intervalMillis;
        }

        @Override
        public int compareTo(RetryEvent o) {
            return Long.compare(this.nextRunTimeMillis, o.nextRunTimeMillis);
        }
    }
}
