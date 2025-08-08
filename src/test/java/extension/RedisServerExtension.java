package extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import server.RedisServer;

public class RedisServerExtension implements BeforeAllCallback, AfterAllCallback, Extension {
    private RedisServer redisServer;

    @Override
    public void beforeAll(ExtensionContext context) throws InterruptedException {
        redisServer = RedisServer.init();
        redisServer.startServer();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        redisServer.stopServer();
    }
}
