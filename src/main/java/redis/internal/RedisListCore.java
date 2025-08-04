package redis.internal;

import java.util.List;

public interface RedisListCore {
    int rpush(String key, List<String> items);

    int lpush(String key, List<String> items);

    List<String> getValue(String key);

    List<String> lrange(String key, int startIdx, int endIdx);

    int size(String key);

    List<String> lpop(String key, int nPop);

    String lpop(String key);

    String blpop(String key, String timeoutSeconds);

}
