package stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class BufferReader implements Reader {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private final ByteBuffer buffer;
    private final ReadableByteChannel channel;

    public BufferReader(ReadableByteChannel channel) {
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        buffer.limit(0); // empty initially
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int readByte() throws IOException {
        fillBuffer();
        if (!buffer.hasRemaining()) {
            return -1;
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder();
        boolean foundCR = false;
        while (true) {
            int ch = readByte();
            if (ch == -1) {
                return null;
            }
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
            if (ch == -1) {
                return null;
            }
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

    public String readAll() throws IOException {
        StringBuilder result = new StringBuilder();

        // First fill buffer with some data
        fillBuffer();

        int emptyReads = 0;
        while (emptyReads < 3) {
            boolean readSomething = false;

            // Drain current buffer
            while (buffer.hasRemaining()) {
                result.append((char) buffer.get());
                readSomething = true;
            }

            // Try to read more from channel
            buffer.clear();
            int bytesRead = channel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                readSomething = true;
            }

            if (!readSomething) {
                emptyReads++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                emptyReads = 0;
            }
        }

        return result.toString();
    }

    private int fillBuffer() throws IOException {
        if (buffer.hasRemaining()) {
            return -1;
        }
        buffer.clear();
        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            // EOF is normal, do not log
            return -1;
        }
        buffer.flip();
        return bytesRead;
    }
}
