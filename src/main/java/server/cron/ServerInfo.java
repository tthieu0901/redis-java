package server.cron;

import helper.ArgumentExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerInfo {
    // Singleton
    // ------------------------------------------------------------------
    private static final ServerInfo INSTANCE = new ServerInfo();

    private ServerInfo() {
    }

    public static ServerInfo getInstance() {
        return INSTANCE;
    }
    // ------------------------------------------------------------------

    private final Map<InfoKey, String> SERVER_INFO = new HashMap<>();

    // Default value
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_HOSTNAME = "localhost";

    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";

    public String get(InfoKey key) {
        return SERVER_INFO.get(key);
    }

    public String getRole() {
        return SERVER_INFO.get(InfoKey.ROLE);
    }

    public void init(String[] args) {
        setDefaultValue();

        // custom configurable values
        setPort(args);
        setMasterInfo(args);
    }

    private void setDefaultValue() {
        SERVER_INFO.put(InfoKey.ROLE, "master");
        SERVER_INFO.put(InfoKey.PORT, String.valueOf(DEFAULT_PORT));
        SERVER_INFO.put(InfoKey.HOST_NAME, DEFAULT_HOSTNAME);
    }

    private void setMasterInfo(String[] args) {
        Optional.ofNullable(ArgumentExtractor.extractByKey(args, "--replicaof"))
                .map(ArgumentExtractor.Pair::value)
                .ifPresent(masterInfo -> {
                    var info = masterInfo.split(" ");
                    SERVER_INFO.put(InfoKey.MASTER_HOSTNAME, info[0]);
                    SERVER_INFO.put(InfoKey.MASTER_PORT, info[1]);
                    SERVER_INFO.put(InfoKey.ROLE, "replica");
                });
    }

    private void setPort(String[] args) {
        int port = Optional.ofNullable(ArgumentExtractor.extractByKey(args, "--port"))
                .map(ArgumentExtractor.Pair::value)
                .map(Integer::parseInt)
                .orElse(DEFAULT_PORT);
        SERVER_INFO.put(InfoKey.PORT, String.valueOf(port));
    }

    public enum InfoKey {
        ROLE,
        HOST_NAME,
        PORT,
        MASTER_PORT,
        MASTER_HOSTNAME,
        ;
    }
}
