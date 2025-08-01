package redis.internal;

import java.util.concurrent.ConcurrentHashMap;

public class RedisStringCore {
    private static final ConcurrentHashMap<String, RedisValue<String>> data = new ConcurrentHashMap<>();

    public static RedisStringCore getInstance() {
        return new RedisStringCore();
    }

    public void set(String key, String value) {
        data.put(key, new RedisValue<>(value));
    }

    public void set(String key, String value, long ttl) {
        data.put(key, new RedisValue<>(value, ttl));
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
}
