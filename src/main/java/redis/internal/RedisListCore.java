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

    public int size(String key) {
        return get(key).size();
    }

    public List<String> lpop(String key, int nPop) {
        var list = get(key);
        if (list.isEmpty()) {
            return List.of();
        }
        if (nPop >= list.size()) {
            data.remove(key);
            return list;
        }
        var deletedList = list.subList(0, nPop);
        data.put(key, new RedisValue<>(list.subList(nPop, list.size())));
        return deletedList;
    }

    public String lpop(String key) {
        var list = get(key);
        if (list.isEmpty()) {
            return null;
        }
        var deleted = list.removeFirst();
        data.put(key, new RedisValue<>(list));
        return deleted;
    }

    public String blpop(String key, String timeout) {
        var list = get(key);
        if (list != null && !list.isEmpty()) {
            return lpop(key);
        }
        return null;
    }
}
