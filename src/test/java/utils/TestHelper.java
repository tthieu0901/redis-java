package utils;

import client.Client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static utils.ConstHelper.REDIS_HOSTNAME;
import static utils.ConstHelper.REDIS_PORT;

public class TestHelper {
    public static void expectError(String expected, String message) {
        assertEquals("-ERR " + expected + "\r\n", message);
    }

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

    public static Client startClient(String hostName, int port) {
        Client client = new Client();
        try {
            client.connect(hostName, port);
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return client;
    }

    public static void stopClient(Client client) {
        try {
            client.disconnect();
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<String> runOnAnotherClient(String hostName, int port, Function<Client, String> handler) {
        return CompletableFuture.supplyAsync(() -> {
            var client = TestHelper.startClient(hostName, port);
            var resp = handler.apply(client);
            TestHelper.stopClient(client);
            return resp;
        });
    }

    public static CompletableFuture<String> startClientAndSendRequest(Function<Client, String> handler) throws InterruptedException {
        var latch = new CountDownLatch(1);
        var client = runOnAnotherClient(REDIS_HOSTNAME, REDIS_PORT, c -> {
            latch.countDown();
            return handler.apply(c);
        });
        latch.await();
        return client;
    }
}
