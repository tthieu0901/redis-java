package redis;

import error.ConnSleepException;
import error.DiscardNoMultiException;
import error.ExecNoMultiException;
import protocol.Protocol;
import redis.internal.NonBlockingRedisListCore;
import redis.internal.NonBlockingRedisStringCore;
import redis.internal.RedisListCore;
import redis.internal.TransactionCore;
import redis.processor.RedisWriteProcessor;
import server.cron.TimeoutCron;
import server.info.ServerInfo;
import stream.Writer;

import java.io.IOException;
import java.util.*;

public class RedisCoreHandler {
    private static final EnumSet<Protocol.Command> TRANSACTION_COMMANDS = EnumSet.of(
            Protocol.Command.MULTI,
            Protocol.Command.EXEC,
            Protocol.Command.DISCARD
    );
    private static final HashMap<String, Queue<Request>> REQUEST_QUEUE = new HashMap<>();

    private final NonBlockingRedisStringCore redisStringCore;
    private final RedisListCore redisListCore;
    private final TransactionCore transactionCore;
    private final Writer writer;

    public RedisCoreHandler(Writer writer) {
        this.writer = writer;
        this.redisStringCore = NonBlockingRedisStringCore.getInstance();
        this.redisListCore = NonBlockingRedisListCore.getInstance();
        this.transactionCore = TransactionCore.getInstance();
    }

    public void handleCommand(List<Object> request) throws IOException {
        var req = request.stream().filter(Objects::nonNull).map(Object::toString).toList();
        var command = new Command(writer.getId(), req);
        validateNumberOfArgs(command, 1);
        var resp = handleCommand(command);
        RedisWriteProcessor.sendResponse(writer, resp);
    }

    private Response handleCommand(Command command) throws IOException {
        var cmd = command.getCmd();
        if (!TRANSACTION_COMMANDS.contains(cmd) && transactionCore.queue(command)) {
            return Response.queued();
        }
        return switch (cmd) {
            case PING -> ping(command);
            case ECHO -> echo(command);
            case SET -> set(command);
            case GET -> get(command);
            case RPUSH -> rpush(command);
            case LPUSH -> lpush(command);
            case LRANGE -> lrange(command);
            case LLEN -> llen(command);
            case LPOP -> lpop(command);
            case BLPOP -> blpop(command);
            case INCR -> incr(command);
            case MULTI -> multi(command);
            case EXEC -> exec(command);
            case DISCARD -> discard(command);
            case INFO -> info(command);
            case REPLCONF -> replconf(command);
            case PSYNC -> psync(command);
        };
    }

    private Response psync(Command ignored) {
        var replId = ServerInfo.getInstance().get(ServerInfo.InfoKey.MASTER_REPL_ID);
        var replOffset = ServerInfo.getInstance().get(ServerInfo.InfoKey.MASTER_REPL_OFFSET);
        return Response.simpleString(String.format("FULLRESYNC %s %s", replId, replOffset));
    }

    private Response replconf(Command ignored) {
        return Response.ok();
    }

    private Response info(Command command) {
        if (command.getRequest().contains("replication")) {
            return Response.bulkString(ServerInfo.getInstance().getAllInfo());
        }
        return Response.bulkString(ServerInfo.getInstance().getAllInfo());
    }

    private Response discard(Command command) {
        try {
            transactionCore.discard(command);
            return Response.ok();
        } catch (DiscardNoMultiException e) {
            return Response.error("DISCARD without MULTI");
        }
    }

    private Response exec(Command command) throws IOException {
        try {
            var commandQueue = transactionCore.exec(command);
            var responses = new ArrayList<Response>();
            while (!commandQueue.isEmpty()) {
                var resp = handleCommand(commandQueue.poll());
                responses.add(resp);
            }
            return Response.arrayResponse(responses);
        } catch (ExecNoMultiException e) {
            return Response.error("EXEC without MULTI");
        }
    }

    private Response multi(Command command) {
        transactionCore.multi(command);
        return Response.ok();
    }

    private Response incr(Command command) {
        validateNumberOfArgs(command, 2);
        var key = command.getKey();
        try {
            var resp = redisStringCore.incr(key);
            return Response.intValue(resp);
        } catch (NumberFormatException e) {
            return Response.error("value is not an integer or out of range");
        }
    }

    private Response blpop(Command command) {
        validateNumberOfArgs(command, 2);
        var key = command.getKey();
        var timeoutSeconds = command.getData().getFirst();

        final Request request;
        if ("0".equals(timeoutSeconds)) { // Wait indefinitely
            request = new Request(writer);
        } else {
            var timeoutMillis = (long) (Double.parseDouble(timeoutSeconds) * 1000L);
            request = new Request(writer, timeoutMillis);
        }

        var resp = redisListCore.lpop(key);
        if (resp != null) {
            return Response.arrayString(List.of(key, resp));
        }

        var queue = REQUEST_QUEUE.computeIfAbsent(key, _ -> new ArrayDeque<>());
        queue.add(request);

        // register to serverCron to handle client timeout
        TimeoutCron.getInstance().registerTimeout(request.getTtlMillis(), () -> {
            try {
                if (REQUEST_QUEUE.get(key).contains(request)) {
                    RedisWriteProcessor.sendNull(writer);
                    REQUEST_QUEUE.get(key).remove(request);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        throw new ConnSleepException();
    }

    private Response lpop(Command command) {
        validateNumberOfArgs(command, 2);
        var key = command.getKey();
        if (command.getData().isEmpty()) {
            return lpopSingleElement(key);
        } else {
            return lpopMultipleElements(command, key);
        }
    }

    private Response lpopMultipleElements(Command command, String key) {
        var nPop = Integer.parseInt(command.getData().getFirst());
        var deletedList = redisListCore.lpop(key, nPop);
        return Response.arrayString(deletedList);
    }

    private Response lpopSingleElement(String key) {
        var resp = redisListCore.lpop(key);
        return resp == null
                ? Response.nullValue()
                : Response.bulkString(resp);
    }

    private Response llen(Command command) {
        validateNumberOfArgs(command, 2);
        var key = command.getKey();
        var len = redisListCore.size(key);
        return Response.intValue(len);
    }

    private Response lrange(Command command) {
        validateNumberOfArgs(command, 4);
        var key = command.getKey();
        var data = command.getData();
        var startIdx = Integer.parseInt(data.getFirst());
        var endIdx = Integer.parseInt(data.get(1));
        var resp = redisListCore.lrange(key, startIdx, endIdx);
        return Response.arrayString(resp);
    }

    private Response lpush(Command command) throws IOException {
        validateNumberOfArgs(command, 3);
        var key = command.getKey();
        var items = command.getData();
        var len = redisListCore.lpush(key, items);
        tryBlop(key);
        return Response.intValue(len);
    }

    private Response rpush(Command command) throws IOException {
        validateNumberOfArgs(command, 3);
        var key = command.getKey();
        var items = command.getData();
        var len = redisListCore.rpush(key, items);
        tryBlop(key);
        return Response.intValue(len);
    }

    private Response ping(Command ignored) {
        return Response.simpleString("PONG");
    }

    private Response get(Command command) {
        validateNumberOfArgs(command, 2);
        var key = command.getKey();
        var value = redisStringCore.get(key);
        return Optional.ofNullable(value)
                .map(Response::bulkString)
                .orElseGet(Response::nullValue);
    }

    private Response set(Command command) {
        validateNumberOfArgs(command, 3);
        var key = command.getKey();
        var data = command.getData();
        var value = data.getFirst();
        int pxIdx = findStringIgnoreCase(data, "px", 1);
        if (pxIdx == -1) {
            redisStringCore.set(key, value);
        } else {
            redisStringCore.set(key, value, Long.parseLong(data.get(pxIdx + 1)));
        }
        return Response.ok();
    }

    private Response echo(Command command) {
        validateNumberOfArgs(command, 2);
        return Response.bulkString(command.getKey());
    }

    public int findStringIgnoreCase(List<String> list, String str, int startIdx) {
        for (int i = startIdx; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(str)) {
                return i;
            }
        }
        return -1;
    }

    private void validateNumberOfArgs(Command command, int minSize) {
        if (command.getRequest().size() < minSize) {
            throw new IllegalArgumentException("Wrong number of arguments for command");
        }
    }

    private void tryBlop(String key) throws IOException {
        var queue = REQUEST_QUEUE.get(key);
        if (queue == null) {
            return;
        }

        while (!queue.isEmpty()) {
            var request = queue.peek();
            var writer = request.getWriter();

            if (request.isTimeout()) {
                RedisWriteProcessor.sendNull(writer);
                queue.poll();
                continue;
            }

            var value = redisListCore.lpop(key);
            if (value == null) {
                break;
            }

            RedisWriteProcessor.sendArray(writer, List.of(key, value));
            queue.poll();
        }
    }
}
