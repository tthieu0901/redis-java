package redis.internal;

import redis.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NonBlockingRedisListCore implements RedisListCore {
    private static final HashMap<String, RedisValue<List<String>>> DATA = new HashMap<>();

    private static final RedisListCore INSTANCE = new NonBlockingRedisListCore();

    public static RedisListCore getInstance() {
        return INSTANCE;
    }

    @Override
    public int rpush(String key, List<String> items) {
        var list = getValueInternal(key);
        list.addAll(items);
        DATA.put(key, new RedisValue<>(list));
        return list.size();
    }

    @Override
    public int lpush(String key, List<String> items) {
        var updatedList = new ArrayList<>(items.reversed());
        var list = getValueInternal(key);
        updatedList.addAll(list);
        DATA.put(key, new RedisValue<>(updatedList));
        return updatedList.size();
    }


    List<String> getValueInternal(String key) {
        var redisValue = DATA.get(key);
        if (redisValue == null) {
            return new ArrayList<>();
        }

        if (redisValue.isExpired()) {
            DATA.remove(key);
            return new ArrayList<>();
        }

        return redisValue.getValue();
    }


    @Override
    public List<String> getValue(String key) {
        var redisValue = DATA.get(key);
        if (redisValue == null) {
            return new ArrayList<>();
        }

        if (redisValue.isExpired()) {
            DATA.remove(key);
            return new ArrayList<>();
        }
        return List.copyOf(redisValue.getValue());
    }

    @Override
    public List<String> lrange(String key, int startIdx, int endIdx) {
        if (endIdx >= 0 && startIdx > endIdx) {
            return List.of();
        }
        var list = getValueInternal(key);
        if (list.isEmpty()) {
            return List.of();
        }
        var start = (startIdx >= 0) ? startIdx : Math.max(0, list.size() + startIdx);
        var end = (endIdx >= 0) ? Math.min(list.size() - 1, endIdx) : Math.max(0, list.size() + endIdx);
        if (start > end) {
            return List.of();
        }
        return list.subList(start, end + 1);
    }

    @Override
    public int size(String key) {
        return getValueInternal(key).size();
    }

    @Override
    public List<String> lpop(String key, int nPop) {
        var list = getValueInternal(key);
        if (list.isEmpty()) {
            return List.of();
        }
        if (nPop >= list.size()) {
            DATA.remove(key);
            return list;
        }
        var deletedList = list.subList(0, nPop);
        DATA.put(key, new RedisValue<>(list.subList(nPop, list.size())));
        return deletedList;
    }

    @Override
    public String lpop(String key) {
        var list = getValueInternal(key);
        return removeFist(key, list);
    }

    private String removeFist(String key, List<String> list) {
        if (list.isEmpty()) {
            return null;
        }

        var deleted = list.removeFirst();
        if (list.isEmpty()) {
            DATA.remove(key);
        } else {
            DATA.put(key, new RedisValue<>(list));
        }
        return deleted;
    }

    @Override
    public String blpop(String key, String timeoutSeconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String blpop(String key, Request request) {
        return lpop(key);
    }
}
