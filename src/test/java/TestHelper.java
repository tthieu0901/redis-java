import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestHelper {
    static void expectNull(String message) {
        assertEquals("$-1\r\n", message);
    }

    static void expectSimpleString(String expected, String message) {
        assertEquals("+" + expected + "\r\n", message);
    }

    static void expectBulkString(String expected, String message) {
        assertEquals(String.format("$%s\r\n%s\r\n", expected.length(), expected), message);
    }

    static void expectInt(int expected, String message) {
        assertEquals(String.format(":%s\r\n", expected), message);
    }

    static void expectArray(List<String> expected, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*%s\r\n", expected.size()));
        for (String str : expected) {
            sb.append(String.format("$%s\r\n%s\r\n", str.length(), str));
        }
        assertEquals(sb.toString(), message);
    }

    static Client startClient(String hostName, int port) {
        Client client = new Client();
        try {
            client.connect(hostName, port);
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return client;
    }

    static void stopClient(Client client) {
        try {
            client.disconnect();
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static CompletableFuture<String> runOnAnotherClient(String hostName, int port, Function<Client, String> handler) {
        return CompletableFuture.supplyAsync(() -> {
            var client = TestHelper.startClient(hostName, port);
            var resp = handler.apply(client);
            TestHelper.stopClient(client);
            return resp;
        });
    }
}
