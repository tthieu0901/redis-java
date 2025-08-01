package stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RedisInputStream extends FilterInputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024;


    private final byte[] buffer;
    private int offset = DEFAULT_BUFFER_SIZE;

    private final InputStream inputStream;

    public RedisInputStream(InputStream inputStream) {
        super(inputStream);
        this.inputStream = inputStream;
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public int readByte() throws IOException {
        fillBuffer();
        return buffer[offset++];
    }

    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder();
        boolean foundCR = false;
        while (true) {
            int ch = readByte();
            if (ch == '\r') {
                foundCR = true;
            } else if (ch == '\n' && foundCR) {
                break;
            } else {
                if (foundCR) {
                    line.append('\r');
                    foundCR = false;
                }
                line.append((char) ch);
            }
        }
        return line.toString();
    }

    private void fillBuffer() throws IOException {
        if (offset < DEFAULT_BUFFER_SIZE) {
            return;
        }
        int bytesRead = inputStream.read(buffer);
        offset = 0;
        if (bytesRead == -1) {
            throw new IllegalStateException("End of stream reached");
        }
    }

    public String readAll() throws IOException {
        fillBuffer();
        return new String(buffer, 0, buffer.length);
    }

    public void waitTillAvailable(int timeoutMs) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        while (inputStream.available() == 0) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new IOException("Timeout waiting for data");
            }
            Thread.sleep(10);
        }
    }
}
