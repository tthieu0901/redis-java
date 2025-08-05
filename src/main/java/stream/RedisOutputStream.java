package stream;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
public class RedisOutputStream implements Writer {
    private final OutputStream out;

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
