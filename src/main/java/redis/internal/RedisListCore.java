package redis.internal;

import java.io.IOException;
import java.util.List;

public interface RedisListCore {
    int rpush(String key, List<String> items) throws IOException;

    int lpush(String key, List<String> items) throws IOException;

    List<String> getValue(String key);

    List<String> lrange(String key, int startIdx, int endIdx);

    int size(String key);

    List<String> lpop(String key, int nPop);

    String lpop(String key);

    String blpop(String key, String timeoutSeconds);

}
