package server.info;

import helper.ArgumentExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    private static final String DEFAULT_MASTER_REP_ID = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"; // hardcode for now

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
        SERVER_INFO.put(InfoKey.MASTER_REPL_ID, DEFAULT_MASTER_REP_ID);
        SERVER_INFO.put(InfoKey.MASTER_REPL_OFFSET, String.valueOf(0));
    }

    private void setMasterInfo(String[] args) {
        Optional.ofNullable(ArgumentExtractor.extractByKey(args, "--replicaof"))
                .map(ArgumentExtractor.Pair::value)
                .ifPresent(masterInfo -> {
                    var info = masterInfo.split(" ");
                    SERVER_INFO.put(InfoKey.MASTER_HOSTNAME, info[0]);
                    SERVER_INFO.put(InfoKey.MASTER_PORT, info[1]);
                    SERVER_INFO.put(InfoKey.ROLE, "slave");
                });
    }

    private void setPort(String[] args) {
        int port = Optional.ofNullable(ArgumentExtractor.extractByKey(args, "--port"))
                .map(ArgumentExtractor.Pair::value)
                .map(Integer::parseInt)
                .orElse(DEFAULT_PORT);
        SERVER_INFO.put(InfoKey.PORT, String.valueOf(port));
    }

    public String get(InfoKey key) {
        return SERVER_INFO.get(key);
    }

    public int getPort() {
        return Integer.parseInt(SERVER_INFO.get(InfoKey.PORT));
    }

    public String getHostName() {
        return SERVER_INFO.get(InfoKey.HOST_NAME);
    }

    public int getMasterPort() {
        return Integer.parseInt(SERVER_INFO.get(InfoKey.MASTER_PORT));
    }

    public String getMasterHostName() {
        return SERVER_INFO.get(InfoKey.MASTER_HOSTNAME);
    }

    public boolean isReplica() {
        return Objects.equals(SERVER_INFO.get(InfoKey.ROLE), "slave");
    }

    public String getAllInfo() {
        return String.join("\n",
                "role:" + SERVER_INFO.get(InfoKey.ROLE),
                "master_replid:" + SERVER_INFO.get(InfoKey.MASTER_REPL_ID),
                "master_repl_offset:" + SERVER_INFO.get(InfoKey.MASTER_REPL_OFFSET)
        );
    }

    public enum InfoKey {
        ROLE,
        HOST_NAME,
        PORT,
        MASTER_PORT,
        MASTER_HOSTNAME,
        MASTER_REPL_ID,
        MASTER_REPL_OFFSET,
        ;
    }
}
