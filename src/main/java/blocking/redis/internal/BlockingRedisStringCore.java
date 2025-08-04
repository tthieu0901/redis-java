package blocking.redis.internal;

import redis.internal.RedisValue;

import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class BlockingRedisStringCore {
    private static final ConcurrentHashMap<String, RedisValue<String>> DATA = new ConcurrentHashMap<>();

    private static final BlockingRedisStringCore INSTANCE = new BlockingRedisStringCore();

    private BlockingRedisStringCore() {
    }

    public static BlockingRedisStringCore getInstance() {
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
