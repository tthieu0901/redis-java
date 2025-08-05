package stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RedisInputStream extends FilterInputStream implements Reader {
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

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public int readByte() throws IOException {
        fillBuffer();
        return buffer[offset++];
    }

    @Override
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

    @Override
    public String readLine(int maxLen) throws IOException {
        StringBuilder line = new StringBuilder();
        boolean foundCR = false;
        while (true) {
            if (line.length() > maxLen) {
                throw new IOException("Too many bytes read");
            }
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

        // First, ensure we have some data
        fillBuffer();

        // Read all available data with retries
        int emptyReads = 0;
        while (emptyReads < 3) { // Allow up to 3 consecutive empty reads
            boolean readSomething = false;

            // Read from current buffer
            while (offset < bytesInBuffer) {
                result.append((char) buffer[offset++]);
                readSomething = true;
            }

            // Try to read more data
            if (inputStream.available() > 0) {
                fillBuffer();
                readSomething = true;
            }

            if (!readSomething) {
                emptyReads++;
                try {
                    Thread.sleep(50); // Wait a bit for more data
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                emptyReads = 0; // Reset counter if we read something
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
            Thread.sleep(100);
        }
    }
}