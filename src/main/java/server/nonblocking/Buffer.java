package server.nonblocking;

public class Buffer {
    private byte[] buffer;
    private int bufferEnd;    // buffer.length

    private int dataBegin;    // start of valid data
    private int dataEnd;      // end of valid data

    // ByteBuffer-style fields
    private int position;     // read position (relative to dataBegin)
    private int mark = -1;

    public Buffer(int initialSize) {
        this.buffer = new byte[initialSize];
        this.bufferEnd = buffer.length;
        this.dataBegin = 0;
        this.dataEnd = 0;
        this.position = 0;
    }

    // ========== Existing Methods ==========

    public int dataSize() {
        return dataEnd - dataBegin;
    }

    public int freeSpace() {
        return bufferEnd - dataEnd;
    }

    public void append(byte[] data, int length) {
        ensureCapacity(length);
        System.arraycopy(data, 0, buffer, dataEnd, length);
        dataEnd += length;
    }

    public void consume(int length) {
        if (length > dataSize() - position) {
            length = dataSize() - position;
        }
        position += length;
        dataBegin += position;
        position = 0;
        mark = -1;

        if (dataBegin > buffer.length / 2) {
            compact();
        }
    }

    private void compact() {
        if (dataBegin > 0) {
            int dataLength = dataSize();
            System.arraycopy(buffer, dataBegin, buffer, 0, dataLength);
            dataBegin = 0;
            dataEnd = dataLength;
        }
        position = 0;
        if (mark >= 0) {
            mark = 0;
        }
    }

    private void ensureCapacity(int additionalBytes) {
        if (freeSpace() < additionalBytes) {
            compact();

            if (freeSpace() < additionalBytes) {
                int newSize = Math.max(buffer.length * 2, dataSize() + additionalBytes);
                byte[] newBuffer = new byte[newSize];
                System.arraycopy(buffer, dataBegin, newBuffer, 0, dataSize());
                buffer = newBuffer;
                bufferEnd = buffer.length;
                dataEnd = dataSize();
                dataBegin = 0;
                position = 0;
                if (mark >= 0) {
                    mark = 0;
                }
            }
        }
    }

    public boolean hasAtLeast(int n) {
        return dataSize() - position >= n;
    }

    public byte getByte(int offset) {
        if (offset >= dataSize()) {
            throw new IndexOutOfBoundsException("Offset beyond data");
        }
        return buffer[dataBegin + offset];
    }

    public byte[] getData() {
        byte[] data = new byte[dataSize()];
        System.arraycopy(buffer, dataBegin, data, 0, dataSize());
        return data;
    }

    public byte[] getData(int offset, int length) {
        if (offset + length > dataSize()) {
            throw new IndexOutOfBoundsException("Not enough data");
        }
        byte[] data = new byte[length];
        System.arraycopy(buffer, dataBegin + offset, data, 0, length);
        return data;
    }

    // ========== ByteBuffer-style Additions ==========

    public int getPosition() {
        return position;
    }

    public void setPosition(int newPos) {
        if (newPos < 0 || newPos > dataSize()) {
            throw new IllegalArgumentException("Invalid position");
        }
        this.position = newPos;
    }

    public int remaining() {
        return dataSize() - position;
    }

    public byte get() {
        if (position >= dataSize()) {
            throw new IndexOutOfBoundsException("No more data");
        }
        return buffer[dataBegin + position++];
    }

    public void get(byte[] dst, int off, int len) {
        if (remaining() < len) {
            throw new IndexOutOfBoundsException("Not enough data");
        }
        System.arraycopy(buffer, dataBegin + position, dst, off, len);
        position += len;
    }

    public void mark() {
        this.mark = this.position;
    }

    public void reset() {
        if (mark < 0) {
            throw new IllegalStateException("No mark set");
        }
        this.position = mark;
    }

    public void rewind() {
        this.position = 0;
        this.mark = -1;
    }

    public void clear() {
        dataBegin = 0;
        dataEnd = 0;
        position = 0;
        mark = -1;
    }

    public boolean hasRemaining() {
        return remaining() > 0;
    }
}
