package stream;

import java.io.IOException;
import java.io.OutputStream;

public class RedisOutputStream extends Writer {
    private final OutputStream out;

    public RedisOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(String str) throws IOException {
        out.write(str.getBytes());
    }

    @Override
    public int flush() throws IOException {
        out.flush();
        return 1;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public boolean hasRemaining() {
        return false;
    }
}
