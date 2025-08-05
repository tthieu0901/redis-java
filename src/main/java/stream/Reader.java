package stream;

import java.io.IOException;

public interface Reader {

    int readByte() throws IOException;

    String readLine() throws IOException;

    String readLine(int maxLen) throws IOException;

    String readAll() throws IOException;

    void close() throws IOException;

    int fillBuffer() throws IOException;
}
