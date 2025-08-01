package redis;

import java.util.concurrent.ConcurrentHashMap;

public class RedisCore {
    private static ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();

    public static RedisCore getInstance() {
        return new RedisCore();
    }

    public void set(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }
}
