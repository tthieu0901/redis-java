package stream;

import java.io.IOException;
import java.util.UUID;

public abstract class Writer {
    private final String id = UUID.randomUUID().toString();

    public final String getId() {
        return id;
    }

    public abstract void write(String str) throws IOException;

    public abstract void write(byte[] bytes) throws IOException;

    public abstract int flush() throws IOException;

    public abstract void close() throws IOException;

    public abstract boolean hasRemaining();
}
