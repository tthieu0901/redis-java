package server.cron;

import java.util.PriorityQueue;

public class TimeoutCron implements ICron {
    // Singleton
    // ------------------------------------------------------------------
    private static final TimeoutCron INSTANCE = new TimeoutCron();

    private TimeoutCron() {
    }

    public static TimeoutCron getInstance() {
        return INSTANCE;
    }
    // ------------------------------------------------------------------

    private final PriorityQueue<TimeoutEvent> timeoutEvents = new PriorityQueue<>();

    @Override
    public void run() {
        while (!timeoutEvents.isEmpty() && timeoutEvents.peek().isTimeout()) {
            TimeoutEvent event = timeoutEvents.poll();
            if (event != null) {
                event.task.run();
            }
        }
    }

    public void registerTimeout(long ttlMillis, Runnable task) {
        timeoutEvents.add(new TimeoutEvent(ttlMillis, task));
    }

    private static class TimeoutEvent implements Comparable<TimeoutEvent> {
        long deadlineMillis;
        Runnable task;

        TimeoutEvent(long delayMillis, Runnable task) {
            this.deadlineMillis = System.currentTimeMillis() + delayMillis;
            this.task = task;
        }

        @Override
        public int compareTo(TimeoutEvent o) {
            return Long.compare(this.deadlineMillis, o.deadlineMillis);
        }

        public boolean isTimeout() {
            return System.currentTimeMillis() >= deadlineMillis;
        }
    }
}
