package redis.internal;

import java.util.List;

public class NonBlockingRedisListCore implements RedisListCore {
    private static final RedisListCore INSTANCE = new NonBlockingRedisListCore();

    public static RedisListCore getInstance() {
        return INSTANCE;
    }

    @Override
    public int rpush(String key, List<String> items) {
        return 0;
    }

    @Override
    public int lpush(String key, List<String> items) {
        return 0;
    }

    @Override
    public List<String> getValue(String key) {
        return List.of();
    }

    @Override
    public List<String> lrange(String key, int startIdx, int endIdx) {
        return List.of();
    }

    @Override
    public int size(String key) {
        return 0;
    }

    @Override
    public List<String> lpop(String key, int nPop) {
        return List.of();
    }

    @Override
    public String lpop(String key) {
        return "";
    }

    @Override
    public String blpop(String key, String timeoutSeconds) {
        return "";
    }
}
