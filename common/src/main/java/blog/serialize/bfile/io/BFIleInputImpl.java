package blog.serialize.bfile.io;

import blog.serialize.bfile.store.FileStoreImpl;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class BFIleInputImpl implements BFileInput {

    private final FileStoreImpl fileStore;
    private ByteBuffer buffer;
    private static final int DEFAULT_BUFFER_SIZE = 512 * 1024;

    public BFIleInputImpl(FileStoreImpl fileStore) {
        this.fileStore = fileStore;
        this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        this.buffer.flip();
    }

    @Override
    public int readInt() {
        require(4);
        return buffer.getInt();
    }

    @Override
    public long readLong() {
        require(8);
        return buffer.getLong();
    }

    @Override
    public short readShort() {
        require(2);
        return buffer.getShort();
    }

    @Override
    public byte readByte() {
        require(1);
        return buffer.get();
    }

    @Override
    public double readDouble() {
        require(8);
        return buffer.getDouble();
    }

    @Override
    public byte[] readBytes() {
        int size = readIntPositiveOptimise();
        require(size);
        byte[] bytes = new byte[size];
        buffer.get(bytes, 0, bytes.length);
        return bytes;
    }

    @Override
    public char readChar() {
        require(2);
        return buffer.getChar();
    }

    @Override
    public void reset(int position) {
        buffer.clear();
        buffer.flip();
        fileStore.seek(position);
    }

    @Override
    public int mark() {
        return fileStore.getPosition() - buffer.remaining();
    }

    @Override
    public void skipBytes(int numBytes) {
        for (int i = 0; i < numBytes; i++) {
            readByte();
        }
    }

    @Override
    public void close() throws IOException {

    }

    public void require(int n) {
        if (buffer.remaining() >= n) return;
        buffer.compact();
        while (buffer.remaining() < n) {
            ByteBuffer b = ByteBuffer.allocate(buffer.capacity() * 2);
            buffer.flip();
            b.put(buffer);
            buffer = b;
        }

        //
        final int r = this.fileStore.read(this.buffer);
        buffer.flip();
        if (r < 0) throw new BufferUnderflowException();
    }

}
