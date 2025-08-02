package redis.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RedisListCore {
    private static final ConcurrentHashMap<String, RedisValue<List<String>>> DATA = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> REQUEST_QUEUE = new ConcurrentLinkedQueue<>();

    public static RedisListCore getInstance() {
        return new RedisListCore();
    }

    public int rpush(String key, List<String> items) {
        var list = new ArrayList<>(getValue(key));
        list.addAll(items);
        var newSize = list.size(); // should store size before update as concurrent update might happen
        DATA.put(key, new RedisValue<>(list));
        return newSize;
    }

    public int lpush(String key, List<String> items) {
        var updatedList = new ArrayList<>(items.reversed());
        var list = getValue(key);
        updatedList.addAll(list);
        var newSize = updatedList.size(); // should store size before update as concurrent update might happen
        DATA.put(key, new RedisValue<>(updatedList));
        return newSize;
    }

    public List<String> getValue(String key) {
        var redisValue = DATA.get(key);
        if (redisValue == null) {
            return List.of();
        }

        if (redisValue.isExpired()) {
            DATA.remove(key);
            return List.of();
        }
        return redisValue.getValue();
    }

    public List<String> lrange(String key, int startIdx, int endIdx) {
        if (endIdx >= 0 && startIdx > endIdx) {
            return List.of();
        }
        var list = getValue(key);
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

    public int size(String key) {
        return getValue(key).size();
    }

    public List<String> lpop(String key, int nPop) {
        var list = getValue(key);
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

    public String lpop(String key) {
        var list = getValue(key);
        return removeFist(key, list);
    }

    public String blpop(String key, int timeout) {
        var list = getValue(key);
        if (list != null && !list.isEmpty()) {
            return removeFist(key, list);
        }
        var requestId = UUID.randomUUID().toString();
        try {
            REQUEST_QUEUE.add(requestId);
            if (timeout == 0) { // wait indefinitely
                while (true) {
                    if (!Objects.equals(REQUEST_QUEUE.peek(), requestId)) {
                        continue;
                    }
                    list = getValue(key);
                    if (!list.isEmpty()) {
                        REQUEST_QUEUE.remove(requestId);
                        return removeFist(key, list);
                    }
                    Thread.sleep(50); // Wait for some time for data to come
                }
            }
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            REQUEST_QUEUE.remove(requestId);
        }
    }

    private String removeFist(String key, List<String> list) {
        if (list.isEmpty()) {
            return null;
        }

        if (list.size() == 1) {
            var deleted = list.getFirst();
            DATA.remove(key);
            return deleted;
        }

        var deleted = list.removeFirst();
        DATA.put(key, new RedisValue<>(list));
        return deleted;
    }
}
