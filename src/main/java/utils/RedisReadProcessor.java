package utils;

import protocol.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class RedisReadProcessor extends RedisProcessor {

    public static String readMessage(InputStream inputStream) {
        Scanner scanner = new Scanner(new InputStreamReader(inputStream));
        scanner.useDelimiter(CRLF);
        return scanner.nextLine();
    }

    private static char readFirstChar(InputStream inputStream) throws IOException {
        return (char) inputStream.read();
    }

    public static List<Object> process(InputStream inputStream) throws IOException {
        var firstChar = readFirstChar(inputStream);
        if (Arrays.stream(Protocol.DataType.values()).noneMatch(dt -> dt.getPrefix() == firstChar)) {
            throw new IOException("Invalid data type");
        }
        var dataType = Protocol.DataType.valueOf(String.valueOf(firstChar));
        return switch (dataType) {
            case BULK_STRING -> List.of(processBulkString(inputStream));
            case ARRAY -> processArray(inputStream);
            default -> throw new IOException("Data type not supported for now: " + dataType);
        };
    }

    private static List<Object> processArray(InputStream inputStream) throws IOException {
        var arrLen = Integer.parseInt(readMessage(inputStream));
        var arr = new ArrayList<>();
        for (int i = 0; i < arrLen; i++) {
            arr.add(process(inputStream));
        }
        return arr;
    }

    private static String processBulkString(InputStream inputStream) {
        var len = Integer.parseInt(readMessage(inputStream)); // for now, this is unnecessary but will change later
        return readMessage(inputStream);
    }
}
