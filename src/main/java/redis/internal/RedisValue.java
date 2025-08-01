package redis.internal;

public class RedisValue<T> {
    private final T value;
    private final long timestamp;
    private final long ttl;
    private final boolean willExpire;

    public RedisValue(T value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.ttl = 0;
        this.willExpire = false;
    }

    public RedisValue(T value, long ttl) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.ttl = ttl;
        this.willExpire = true;
    }

    public T getValue() {
        return value;
    }

    public long getExpiryTime() {
        return timestamp + ttl;
    }

    public boolean isExpired() {
        return willExpire && getExpiryTime() < System.currentTimeMillis();
    }
}
