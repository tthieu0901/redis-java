package redis.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RedisListCore {
    private static final ConcurrentHashMap<String, RedisValue<List<String>>> data = new ConcurrentHashMap<>();

    public static RedisListCore getInstance() {
        return new RedisListCore();
    }

    public int rpush(String key, List<String> items) {
        var list = Optional.ofNullable(get(key))
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        list.addAll(items);
        data.put(key, new RedisValue<>(list));
        return list.size();
    }

    public List<String> get(String key) {
        var redisValue = data.get(key);
        if (redisValue == null) {
            return List.of();
        }

        if (redisValue.isExpired()) {
            data.remove(key);
            return List.of();
        }
        return redisValue.getValue();
    }

    public List<String> lrange(String key, int startIdx, int endIdx) {
        if (startIdx > endIdx) {
            return List.of();
        }
        var list = get(key);
        return list.subList(startIdx, Math.min(list.size(), endIdx + 1));
    }
}
