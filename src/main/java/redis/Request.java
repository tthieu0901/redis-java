package redis;

import lombok.Getter;
import stream.Writer;

public class Request {
    private static final long DEFAULT_TIMEOUT = 5000 * 1000; // 5000s
    private final long timestamp;
    private final long deadline;
    @Getter
    private final Writer writer;
    @Getter
    private final long ttlMillis;

    public Request(Writer writer, long ttlMillis) {
        this.writer = writer;
        this.timestamp = System.currentTimeMillis();
        this.ttlMillis = ttlMillis;
        this.deadline = timestamp + ttlMillis;
    }

    public Request(Writer writer) {
        this.writer = writer;
        this.timestamp = System.currentTimeMillis();
        this.ttlMillis = DEFAULT_TIMEOUT;
        this.deadline = this.timestamp + DEFAULT_TIMEOUT;
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() >= this.deadline;
    }
}
