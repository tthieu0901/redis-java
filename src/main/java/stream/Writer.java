package stream;

import java.io.IOException;

public interface Writer {
    void write(String str) throws IOException;

    int flush() throws IOException;

    void close() throws IOException;

    boolean hasRemaining();
}
