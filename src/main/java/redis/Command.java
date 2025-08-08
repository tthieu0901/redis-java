package redis;

import protocol.Protocol;

import java.util.List;
import java.util.Optional;

public class Command {
    private final String connectionId;
    private final List<String> request;

    public Command(String connectionId, List<String> request) {
        this.connectionId = connectionId;
        this.request = Optional.ofNullable(request).orElse(List.of());
    }

    public List<String> getRequest() {
        return request;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Protocol.Command getCmd() {
        var command = request.getFirst();
        return Optional.ofNullable(Protocol.Command.findCommand(command))
                .orElseThrow(() -> new IllegalArgumentException("Invalid command received: " + command));
    }

    public String getKey() {
        return request.get(1);
    }

    public List<String> getData() {
        return request.subList(2, request.size());
    }
}
