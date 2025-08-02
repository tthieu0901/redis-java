package redis.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RedisListCore {
    private static final ConcurrentHashMap<String, RedisValue<List<String>>> DATA = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> REQUEST_QUEUE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> LOCK = new ConcurrentHashMap<>();

    public static RedisListCore getInstance() {
        return new RedisListCore();
    }

    /**
     * Since we use the same state for multiple thread, we must use synchronized to avoid race condition!
     * For better performance, we lock based on key.
     * <p>
     * Redis use Event Loop so they don't have to deal with this :(
     * <p>
     * TODO: Try to use Event Loop in the future
     */
    private static Object getLock(String key) {
        return LOCK.computeIfAbsent(key, _ -> new Object());
    }

    public int rpush(String key, List<String> items) {
        synchronized (getLock(key)) {
            var list = getValueInternal(key);
            list.addAll(items);

            DATA.put(key, new RedisValue<>(list));

            var queue = REQUEST_QUEUE.get(key);
            if (queue != null && !queue.isEmpty()) {
                queue.notifyAll(); // notify other on-waiting processes
            }

            return list.size();
        }
    }

    public int lpush(String key, List<String> items) {
        synchronized (getLock(key)) {
            var updatedList = new ArrayList<>(items.reversed());
            var list = getValueInternal(key);
            updatedList.addAll(list);

            DATA.put(key, new RedisValue<>(updatedList));

            getLock(key).notifyAll(); // notify other blocking blpop

            return updatedList.size();
        }
    }


    /**
     * This method is for get-then-act operations to reduce lock and copy snapshot too many times
     */
    List<String> getValueInternal(String key) {
        var redisValue = DATA.get(key);
        if (redisValue == null) {
            return new ArrayList<>();
        }

        if (redisValue.isExpired()) {
            DATA.remove(key);
            return new ArrayList<>();
        }

        return new ArrayList<>(redisValue.getValue()); // try to copy the get the current snapshot, might affect performance if the list is too large
    }


    /**
     * getValue return an unmodified snapshot of data wrapped by synchronized keyword.
     * <p>
     * This helps other read-only operations being thread safe (might stale if that operation takes too long to perform, but this is acceptable)
     * <p>
     * Other get-then-act operations still need to be wrapped by synchronized to avoid race condition
     */
    public List<String> getValue(String key) {
        synchronized (getLock(key)) {
            var snapShot = getValueInternal(key);
            return Collections.unmodifiableList(snapShot); // Collections.unmodifiableList restricts add/remove operations but inside list can still be modified
        }
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
        synchronized (getLock(key)) {
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
    }

    public String lpop(String key) {
        synchronized (getLock(key)) {
            var list = getValueInternal(key);
            return removeFist(key, list);
        }
    }

    public String blpop(String key, int timeout) {
        synchronized (getLock(key)) {
            var list = getValueInternal(key);
            if (!list.isEmpty()) {
                return removeFist(key, list);
            }
            var queue = REQUEST_QUEUE.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>());

            var requestId = UUID.randomUUID().toString();
            queue.add(requestId);
            try {
                if (timeout == 0) { // wait indefinitely
                    while (true) {
                        if (!Objects.equals(queue.peek(), requestId)) {
                            continue;
                        }

                        list = getValueInternal(key);
                        if (!list.isEmpty()) {
                            return removeFist(key, list);
                        }

                        getLock(key).wait(50); // Wait for some time to avoid busy waiting (100% CPU run all the time)
                    }
                }
                return null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                queue.remove(requestId);
            }
        }
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
