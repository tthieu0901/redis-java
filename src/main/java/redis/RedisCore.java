package redis;

import java.util.concurrent.ConcurrentHashMap;

public class RedisCore {
    private static ConcurrentHashMap<String, RedisValue> data = new ConcurrentHashMap<>();

    public static RedisCore getInstance() {
        return new RedisCore();
    }

    public void set(String key, String value) {
        data.put(key, new RedisValue(value));
    }

    public void set(String key, String value, long ttl) {
        data.put(key, new RedisValue(value, ttl));
    }

    public String get(String key) {
        var redisValue = data.get(key);
        if (redisValue == null) {
            return null;
        }

        if (redisValue.isExpired()) {
            data.remove(key);
            return null;
        }
        return redisValue.getValue();
    }

    public static class RedisValue {
        private final String value;
        private final long timestamp;
        private final long ttl;
        private final boolean willExpire;

        public RedisValue(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.ttl = 0;
            this.willExpire = false;
        }

        public RedisValue(String value, long ttl) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
            this.willExpire = true;
        }

        public String getValue() {
            return value;
        }

        public long getExpiryTime() {
            return timestamp + ttl;
        }

        public boolean isExpired() {
            return willExpire && getExpiryTime() < System.currentTimeMillis();
        }
    }
}
