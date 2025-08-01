package redis.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RedisListCore {
    private static final ConcurrentHashMap<String, RedisValue<List<String>>> data = new ConcurrentHashMap<>();

    public static RedisListCore getInstance() {
        return new RedisListCore();
    }

    public int rpush(String key, List<String> items) {
        var list = new ArrayList<>(get(key));
        list.addAll(items);
        data.put(key, new RedisValue<>(list));
        return list.size();
    }

    public int lpush(String key, List<String> items) {
        var updatedList = new ArrayList<>(items.reversed());
        var list = get(key);
        updatedList.addAll(list);
        data.put(key, new RedisValue<>(updatedList));
        return updatedList.size();
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
        if (endIdx >= 0 && startIdx > endIdx) {
            return List.of();
        }
        var list = get(key);
        var start = (startIdx >= 0) ?  startIdx : Math.max(0, list.size() + startIdx);
        var end = (endIdx >= 0) ? Math.min(list.size() - 1, endIdx): Math.max(0, list.size() + endIdx);
        if (start > end) {
            return List.of();
        }
        return list.subList(start, end + 1);
    }

    public int llen(String key) {
        return get(key).size();
    }
}
