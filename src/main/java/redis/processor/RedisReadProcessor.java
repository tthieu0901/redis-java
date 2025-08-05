package redis.processor;

import protocol.Protocol;
import stream.Reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RedisReadProcessor {

    public static String readMessage(Reader reader) throws IOException {
        return reader.readLine();
    }

    public static String readMessage(Reader reader, int maxLen) throws IOException {
        return reader.readLine(maxLen);
    }

    private static int readFirstByte(Reader reader) throws IOException {
        return reader.readByte();
    }

    public static List<Object> read(Reader reader) throws IOException {
        int firstByte = readFirstByte(reader);
        var dataType = Protocol.DataType.findDataTypeByPrefix((char) firstByte);
        if (dataType == null) {
            throw new IOException("Invalid data type");
        }
        return switch (dataType) {
            case SIMPLE_STRING -> List.of(readMessage(reader));
            case BULK_STRING -> List.of(readBulkString(reader));
            case ARRAY -> readArray(reader);
            default -> throw new IOException("Data type not supported for now: " + dataType.name());
        };
    }

    private static List<Object> readArray(Reader reader) throws IOException {
        var arrLen = Integer.parseInt(readMessage(reader));
        var arr = new ArrayList<>();
        for (int i = 0; i < arrLen; i++) {
            arr.addAll(read(reader));
        }
        return arr;
    }

    private static String readBulkString(Reader reader) throws IOException {
        var maxLen = Integer.parseInt(readMessage(reader));
        return readMessage(reader, maxLen);
    }
}
