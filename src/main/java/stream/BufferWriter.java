package stream;

import server.nonblocking.Buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufferWriter extends Writer {
    private final WritableByteChannel channel;
    private final Charset charset;
    private final Buffer outgoing;

    public BufferWriter(WritableByteChannel channel, Buffer outgoing) {
        this.channel = channel;
        this.charset = StandardCharsets.UTF_8;
        this.outgoing = outgoing;
    }

    // Queue of buffers waiting to be sent

    @Override
    public void write(String str) {
        var data = str.getBytes(charset);
        if (data.length > 0) {
            outgoing.append(data, data.length);
        }
    }

    @Override
    public int flush() throws IOException {
        byte[] data = outgoing.getData();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        var written = channel.write(buffer);
        if (written > 0) {
            outgoing.consume(written);
        }
        return written;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public boolean hasRemaining() {
        return outgoing.dataSize() > 0;
    }
}
