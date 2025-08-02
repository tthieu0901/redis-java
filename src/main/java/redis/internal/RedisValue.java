package redis.internal;

class RedisValue<T> {
    private final T value;
    private final long timestamp;
    private final long ttl;
    private final boolean willExpire;

    RedisValue(T value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.ttl = 0;
        this.willExpire = false;
    }

    RedisValue(T value, long ttl) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.ttl = ttl;
        this.willExpire = true;
    }

    T getValue() {
        return value;
    }

    long getExpiryTime() {
        return timestamp + ttl;
    }

    boolean isExpired() {
        return willExpire && getExpiryTime() < System.currentTimeMillis();
    }
}
