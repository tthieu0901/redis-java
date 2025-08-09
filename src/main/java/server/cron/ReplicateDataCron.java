package server.cron;

import redis.Command;
import redis.processor.RedisWriteProcessor;
import server.dto.Conn;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ReplicateDataCron implements ICron {
    // Singleton
    // ------------------------------------------------------------------
    private static final ReplicateDataCron INSTANCE = new ReplicateDataCron();

    private ReplicateDataCron() {
    }

    public static ReplicateDataCron getInstance() {
        return INSTANCE;
    }
    // ------------------------------------------------------------------

    private final Queue<ReplicateEvent> replicateEvents = new ArrayDeque<>();
    private final List<Conn> replicas = new ArrayList<>();

    @Override
    public void run() throws IOException {
        while (!replicateEvents.isEmpty()) {
            ReplicateEvent event = replicateEvents.poll();
            for (var replica : replicas) {
                try {
                    RedisWriteProcessor.sendArray(replica.getWriter(), event.command.getRequest());
                }catch (Exception e){
                    System.err.println("Failed to replicate data to replica " + replica.getChannel().getLocalAddress());
                }
            }
        }
    }

    public void addReplica(Conn conn) {
        if (!replicas.contains(conn)) {
            replicas.add(conn);
        }
    }

    public void removeReplica(Conn conn) {
        replicas.remove(conn);
    }

    public void registerCommand(Command command) {
        replicateEvents.add(new ReplicateEvent(command));
    }

    private static class ReplicateEvent {
        Command command;

        ReplicateEvent(Command command) {
            this.command = command;
        }
    }
}
