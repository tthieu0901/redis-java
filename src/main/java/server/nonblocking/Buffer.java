package server.nonblocking;

public class Buffer {
    private byte[] buffer;
    private int bufferBegin;    // always 0 in our implementation
    private int bufferEnd;      // buffer.length
    private int dataBegin;      // start of valid data
    private int dataEnd;        // end of valid data

    public Buffer(int initialSize) {
        this.buffer = new byte[initialSize];
        this.bufferBegin = 0;
        this.bufferEnd = buffer.length;
        this.dataBegin = 0;
        this.dataEnd = 0;
    }

    // Get the amount of valid data
    public int dataSize() {
        return dataEnd - dataBegin;
    }

    // Get the amount of free space at the end
    public int freeSpace() {
        return bufferEnd - dataEnd;
    }

    // Get data as byte array
    public byte[] getData() {
        byte[] data = new byte[dataSize()];
        System.arraycopy(buffer, dataBegin, data, 0, dataSize());
        return data;
    }

    // Get data from a specific offset with length
    public byte[] getData(int offset, int length) {
        if (offset + length > dataSize()) {
            throw new IndexOutOfBoundsException("Not enough data");
        }
        byte[] data = new byte[length];
        System.arraycopy(buffer, dataBegin + offset, data, 0, length);
        return data;
    }

    // Append data to the buffer
    public void append(byte[] data, int length) {
        ensureCapacity(length);
        System.arraycopy(data, 0, buffer, dataEnd, length);
        dataEnd += length;
    }

    // Consume data from the front
    public void consume(int length) {
        if (length > dataSize()) {
            length = dataSize();
        }
        dataBegin += length;

        // Compact buffer if we've consumed more than half
        if (dataBegin > buffer.length / 2) {
            compact();
        }
    }

    // Compact buffer by moving data to the beginning
    private void compact() {
        if (dataBegin > 0) {
            int dataLength = dataSize();
            System.arraycopy(buffer, dataBegin, buffer, 0, dataLength);
            dataBegin = 0;
            dataEnd = dataLength;
        }
    }

    // Ensure buffer has enough capacity for additional data
    private void ensureCapacity(int additionalBytes) {
        if (freeSpace() < additionalBytes) {
            // Try compacting first
            compact();

            // If still not enough space, resize
            if (freeSpace() < additionalBytes) {
                int newSize = Math.max(buffer.length * 2, dataSize() + additionalBytes);
                byte[] newBuffer = new byte[newSize];
                System.arraycopy(buffer, dataBegin, newBuffer, 0, dataSize());
                buffer = newBuffer;
                bufferEnd = buffer.length;
                dataEnd = dataSize();
                dataBegin = 0;
            }
        }
    }

    // Check if buffer has at least n bytes of data
    public boolean hasAtLeast(int n) {
        return dataSize() >= n;
    }

    // Get byte at specific offset from data start
    public byte getByte(int offset) {
        if (offset >= dataSize()) {
            throw new IndexOutOfBoundsException("Offset beyond data");
        }
        return buffer[dataBegin + offset];
    }
}
