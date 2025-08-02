package redis.processor;

import protocol.Protocol;
import stream.RedisInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RedisReadProcessor {

    public static String readMessage(RedisInputStream inputStream) throws IOException {
        return inputStream.readLine();
    }

    public static String readMessage(RedisInputStream inputStream, int maxLen) throws IOException {
        return inputStream.readLine(maxLen);
    }

    private static char readFirstByte(RedisInputStream inputStream) throws IOException {
        return (char) inputStream.readByte();
    }

    public static List<Object> read(RedisInputStream inputStream) throws IOException {
        var firstChar = readFirstByte(inputStream);
        var dataType = Protocol.DataType.findDataTypeByPrefix(firstChar);
        if (dataType == null) {
            throw new IOException("Invalid data type");
        }
        return switch (dataType) {
            case SIMPLE_STRING -> List.of(readMessage(inputStream));
            case BULK_STRING -> List.of(readBulkString(inputStream));
            case ARRAY -> readArray(inputStream);
            default -> throw new IOException("Data type not supported for now: " + dataType.name());
        };
    }

    private static List<Object> readArray(RedisInputStream inputStream) throws IOException {
        var arrLen = Integer.parseInt(readMessage(inputStream));
        var arr = new ArrayList<>();
        for (int i = 0; i < arrLen; i++) {
            arr.addAll(read(inputStream));
        }
        return arr;
    }

    private static String readBulkString(RedisInputStream inputStream) throws IOException {
        var maxLen = Integer.parseInt(readMessage(inputStream));
        return readMessage(inputStream, maxLen);
    }
}
