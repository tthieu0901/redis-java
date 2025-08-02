package redis.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RedisListCore {
    private static final ConcurrentHashMap<String, RedisValue<List<String>>> DATA = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> REQUEST_QUEUE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Condition> CONDITIONS = new ConcurrentHashMap<>();

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
    private ReentrantLock getLock(String key) {
        return LOCKS.computeIfAbsent(key, _ -> new ReentrantLock());
    }
    
    private Condition getCondition(String key) {
        return CONDITIONS.computeIfAbsent(key, _ -> getLock(key).newCondition());
    }

    public int rpush(String key, List<String> items) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            var list = getValueInternal(key);
            list.addAll(items);
            DATA.put(key, new RedisValue<>(list));
            
            // Notify all waiting blpop operations
            getCondition(key).signalAll();
            
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    public int lpush(String key, List<String> items) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            var updatedList = new ArrayList<>(items.reversed());
            var list = getValueInternal(key);
            updatedList.addAll(list);
            DATA.put(key, new RedisValue<>(updatedList));
            
            // Notify all waiting blpop operations
            getCondition(key).signalAll();
            
            return updatedList.size();
        } finally {
            lock.unlock();
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
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            var snapShot = getValueInternal(key);
            return Collections.unmodifiableList(snapShot);
        } finally {
            lock.unlock();
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
        var queue = REQUEST_QUEUE.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>());
        var requestId = UUID.randomUUID().toString();
        
        ReentrantLock lock = getLock(key);
        Condition condition = getCondition(key);
        
        lock.lock();
        try {
            // First check if data is immediately available
            var list = getValueInternal(key);
            if (!list.isEmpty()) {
                return removeFist(key, list);
            }
            
            // Add to queue
            queue.add(requestId);
            
            // Wait for data
            while (true) {
                // Check if we're first in queue
                if (!Objects.equals(queue.peek(), requestId)) {
                    condition.await();
                    continue;
                }
                
                // We're first in queue, check for data
                list = getValueInternal(key);
                if (!list.isEmpty()) {
                    queue.remove(requestId);
                    return removeFist(key, list);
                }
                
                // No data available, wait for notification
                if (timeout == 0) {
                    condition.await();
                } else {
                    // Handle timeout case if needed
                    return null;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            queue.remove(requestId);
            lock.unlock();
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