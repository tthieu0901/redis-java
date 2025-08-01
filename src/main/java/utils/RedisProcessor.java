package utils;

public class RedisProcessor {
    public static final String CRLF = "\r\n";

    public static String sanitizeData(String data) {
        return data.replaceAll("\r\n", "");
    }
}
