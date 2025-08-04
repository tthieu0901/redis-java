package redis.internal;

import java.util.*;

public class NonBlockingRedisListCore implements RedisListCore {
    private static final HashMap<String, RedisValue<List<String>>> DATA = new HashMap<>();
    private static final HashMap<String, Queue<String>> REQUEST_QUEUE = new HashMap<>();

    private static final RedisListCore INSTANCE = new NonBlockingRedisListCore();

    public static RedisListCore getInstance() {
        return INSTANCE;
    }

    @Override
    public int rpush(String key, List<String> items) {
        var list = getValueInternal(key);
        list.addAll(items);
        DATA.put(key, new RedisValue<>(list));
        // TODO: notify BLPOP
        return list.size();
    }

    @Override
    public int lpush(String key, List<String> items) {
        var updatedList = new ArrayList<>(items.reversed());
        var list = getValueInternal(key);
        updatedList.addAll(list);
        DATA.put(key, new RedisValue<>(updatedList));
        // TODO: notify BLPOP
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

    @Override
    public String blpop(String key, String timeoutSeconds) {
        // TODO: Refine this logic
        var queue = REQUEST_QUEUE.computeIfAbsent(key, _ -> new ArrayDeque<>());
        var requestId = UUID.randomUUID().toString();
        try {
            // First check if data is immediately available
            var list = getValueInternal(key);
            if (!list.isEmpty()) {
                return removeFist(key, list);
            }

            // Add to queue
            queue.add(requestId);

            // Check if we're first in queue
            if (!Objects.equals(queue.peek(), requestId)) {
//                    condition.await();
                return null;
            }

            // We're first in queue, check for data
            var value = getValueInternal(key);
            if (!value.isEmpty()) {
                return removeFist(key, value);
            }

            // No data available, wait for notification
            if ("0".equals(timeoutSeconds)) {
//                    condition.await();
            } else {
//                    if (!condition.await((long) (Double.parseDouble(timeoutSeconds) * 1000), TimeUnit.MILLISECONDS)) {
//                        return null;
//                    }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            queue.remove(requestId);
        }
        return null;
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
}
