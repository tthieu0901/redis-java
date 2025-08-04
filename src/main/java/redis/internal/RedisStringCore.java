package redis.internal;

import java.util.concurrent.ConcurrentHashMap;

public class RedisStringCore {
    private static final ConcurrentHashMap<String, RedisValue<String>> DATA = new ConcurrentHashMap<>();

    private static final RedisStringCore INSTANCE = new RedisStringCore();

    private RedisStringCore() {
    }

    public static RedisStringCore getInstance() {
        return INSTANCE;
    }

    public void set(String key, String value) {
        DATA.put(key, new RedisValue<>(value));
    }

    public void set(String key, String value, long ttl) {
        DATA.put(key, new RedisValue<>(value, ttl));
    }

    public String get(String key) {
        var redisValue = DATA.get(key);
        if (redisValue == null) {
            return null;
        }

        if (redisValue.isExpired()) {
            DATA.remove(key);
            return null;
        }
        return redisValue.getValue();
    }
}
