package stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RedisInputStream extends FilterInputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private byte[] buffer;
    private int offset = DEFAULT_BUFFER_SIZE;
    private int bytesInBuffer = 0;

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
        if (offset < bytesInBuffer) {
            return;
        }
        bytesInBuffer = inputStream.read(buffer);
        offset = 0;
        if (bytesInBuffer == -1) {
            throw new IllegalStateException("End of stream reached");
        }
    }

    public String readAll() throws IOException {
        StringBuilder result = new StringBuilder();

        // Read all available data
        while (inputStream.available() > 0 || offset < bytesInBuffer) {
            if (offset >= bytesInBuffer) {
                fillBuffer();
            }
            if (bytesInBuffer > 0) {
                result.append((char) buffer[offset++]);
            }
        }

        return result.toString();
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