package stream;

import java.io.IOException;

public interface Writer {
    void write(String str) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
