package stream;

import java.io.IOException;

public interface Reader {

    int readByte() throws IOException;

    String readLine() throws IOException;

    String readLine(int maxLen) throws IOException;

    default String readAll() throws IOException {
        throw new UnsupportedOperationException("readAll not implemented");
    }

    default void mark() {
        throw new UnsupportedOperationException("mark not implemented");
    }

    default void reset() throws IOException {
        throw new UnsupportedOperationException("reset not implemented");
    }

    void close() throws IOException;

    default void commit() {
        throw new UnsupportedOperationException("consume not implemented");
    }
}
