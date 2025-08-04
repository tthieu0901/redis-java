package stream;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

@RequiredArgsConstructor
public class BufferWriter implements Writer {
    private final WritableByteChannel channel;
    private final Charset charset;

    public BufferWriter(WritableByteChannel channel) {
        this.channel = channel;
        this.charset = StandardCharsets.UTF_8;
    }

    // Queue of buffers waiting to be sent
    private final Queue<ByteBuffer> pending = new ArrayDeque<>();

    @Override
    public void write(String str) throws IOException {
        ByteBuffer buffer = charset.encode(str);
        pending.add(buffer);
        flush(); // attempt immediate send
    }

    @Override
    public void flush() throws IOException {
        while (!pending.isEmpty()) {
            ByteBuffer buffer = pending.peek();
            channel.write(buffer); // may write partially in non-blocking mode
            if (buffer.hasRemaining()) {
                // Channel can't accept more right now, stop flushing
                break;
            }
            pending.poll(); // fully sent, remove from queue
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
