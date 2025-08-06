package redis.internal;

import java.util.HashMap;

public class NonBlockingRedisStringCore {
    private static final HashMap<String, RedisValue<String>> DATA = new HashMap<>();

    private static final NonBlockingRedisStringCore INSTANCE = new NonBlockingRedisStringCore();

    private NonBlockingRedisStringCore() {
    }

    public static NonBlockingRedisStringCore getInstance() {
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

    public String incr(String key) {
        var value = get(key);
        if (value != null) {
            set(key, String.valueOf(Integer.parseInt(value) + 1));
        } else {
            set(key, "1");
        }
        return get(key);
    }
}
