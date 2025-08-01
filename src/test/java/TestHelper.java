import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHelper {
    public static void expectNull(String message) {
        assertEquals("$-1\r\n", message);
    }

    public static void expectSimpleString(String expected, String message) {
        assertEquals("+" + expected + "\r\n", message);
    }

    public static void expectBulkString(String expected, String message) {
        assertEquals(String.format("$%s\r\n%s\r\n", expected.length(), expected), message);
    }

    public static void expectInt(int expected, String message) {
        assertEquals(String.format(":%s\r\n", expected), message);
    }

    public static void expectArray(List<String> expected, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*%s\r\n", expected.size()));
        for (String str : expected) {
            sb.append(String.format("$%s\r\n%s\r\n", str.length(), str));
        }
        assertEquals(sb.toString(), message);
    }

}
