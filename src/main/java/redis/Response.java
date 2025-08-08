package redis;

import java.util.List;

public record Response(Object message, ResponseType responseType) {
    private static final Response NULL_RESPONSE = new Response(null, ResponseType.NULL);
    private static final Response OK_RESPONSE = simpleString("OK");
    private static final Response QUEUED_RESPONSE = simpleString("QUEUED");

    public enum ResponseType {
        STRING,
        BULK_STRING,
        INTEGER,
        NULL,
        ERROR,
        ARRAY_STRING,
        ARRAY_RESPONSE,
        ;
    }

    public static Response ok() {
        return OK_RESPONSE;
    }

    public static Response queued() {
        return QUEUED_RESPONSE;
    }

    public static Response simpleString(String message) {
        return new Response(message, ResponseType.STRING);
    }

    public static Response error(String message) {
        return new Response(message, ResponseType.ERROR);
    }

    public static Response bulkString(String message) {
        return new Response(message, ResponseType.BULK_STRING);
    }

    public static Response nullValue() {
        return NULL_RESPONSE;
    }

    public static Response intValue(String message) {
        return new Response(Integer.parseInt(message), ResponseType.INTEGER);
    }

    public static Response intValue(int message) {
        return new Response(message, ResponseType.INTEGER);
    }

    public static Response arrayString(List<String> messages) {
        return new Response(messages, ResponseType.ARRAY_STRING);
    }

    public static Response arrayResponse(List<Response> messages) {
        return new Response(messages, ResponseType.ARRAY_RESPONSE);
    }
}
