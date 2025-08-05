package stream;

import error.NotEnoughDataException;
import server.nonblocking.Buffer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class BufferReader implements Reader {

    private static final int DEFAULT_BUFFER_SIZE = 64 * 1024; // 64KB

    private final ReadableByteChannel channel;
    private final Buffer incoming;
    private final ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    private int totalBytesRead = 0;

    public BufferReader(ReadableByteChannel channel, Buffer incoming) {
        this.channel = channel;
        this.incoming = incoming;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int readByte() throws IOException {
        while (!incoming.hasAtLeast(1)) {
            int bytesRead = fillBuffer();
            if (bytesRead == 0) {
                throw new NotEnoughDataException();
            }
            if (bytesRead == -1) {
                throw new EOFException("EOF Reached");
            }
        }
        totalBytesRead++;
        var data = incoming.getByte(incoming.getPosition()) & 0xFF;
        incoming.setPosition(incoming.getPosition() + 1);
        return data & 0xFF;
    }

    @Override
    public String readLine() throws IOException {
        return readLine(-1);
    }

    @Override
    public String readLine(int len) throws IOException {
        StringBuilder line = new StringBuilder();
        boolean foundCR = false;
        while (true) {
            if (len >= 0 && line.length() > len) {
                throw new IOException("Too many bytes read");
            }
            var ch = readByte();
            // handle CRLF
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
        if (line.length() < len) {
            throw new NotEnoughDataException();
        }
        return line.toString();
    }

    @Override
    public void mark() {
        incoming.mark();
    }

    @Override
    public void reset() {
        incoming.reset();
        totalBytesRead = 0;
    }

    @Override
    public void consume() {
        incoming.consume(totalBytesRead);
    }

    private int fillBuffer() throws IOException {
        buffer.clear();
        int bytesRead = channel.read(buffer);

        if (bytesRead == 0) {
            return 0; // actually not ready (would block)
        }

        if (bytesRead < 0) {
            if (incoming.dataSize() == 0) {
                System.out.println("Client disconnected");
            } else {
                System.out.println("Unexpected EOF");
            }
            return -1;
        }

        buffer.flip();
        byte[] data = new byte[bytesRead];
        buffer.get(data);
        incoming.append(data, bytesRead);
        return bytesRead;
    }
}
