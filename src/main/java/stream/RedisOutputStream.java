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
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
