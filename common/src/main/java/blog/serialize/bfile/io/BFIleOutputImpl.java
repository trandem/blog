package blog.serialize.bfile.io;

import blog.serialize.base.DOutput;
import blog.serialize.bfile.store.FileStoreImpl;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class BFIleOutputImpl implements BFileOutput {

    private FileStoreImpl fileStore;
    private ByteBuffer buffer;
    private static final int DEFAULT_BUFFER_SIZE = 512 * 1024;

    public BFIleOutputImpl(FileStoreImpl fileStore) {
        this.fileStore = fileStore;
//        this.buffer = ByteBuffer.allocate(fileStore.getCapacity());
        this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        this.buffer.clear();
    }

    @Override
    public void writeInt(int data) {
        require(4);
        buffer.putInt(data);
    }

    @Override
    public DOutput clear() {
        buffer.clear();
        return this;
    }

    @Override
    public void writeLong(long data) {
        require(8);
        buffer.putLong(data);
    }

    @Override
    public void writeShort(short data) {
        require(2);
        buffer.putShort(data);
    }

    @Override
    public void writeByte(byte data) {
        require(1);
        buffer.put(data);
    }

    @Override
    public void writeBytes(byte[] data) {
        int size = data.length;
        writeIntOptimise(size);
        require(size);
        buffer.put(data);
    }

    @Override
    public void writeDouble(double data) {
        require(8);
        buffer.putDouble(data);
    }

    @Override
    public void writeChar(char data) {
        require(2);
        buffer.putChar(data);
    }

    @Override
    public byte[] toArrayBytes() {
        byte[] a = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(a);
        return a;
    }

    @Override
    public void reset(int position) {
        buffer.position(position);
    }

    @Override
    public int mark() {
        return buffer.position();
    }

    @Override
    public void flush() {
        this.buffer.flip();// ready for read data
        if (buffer.remaining() > 0) fileStore.write(buffer);
        this.buffer.clear(); /* prepares for writing */
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    private void require(int n) {
        if (buffer.remaining() >= n) return;
        if (buffer.position() + n > fileStore.getRemainCap()) {
            throw new BufferOverflowException();
        }

        while (buffer.remaining() < n) {
            ByteBuffer b = ByteBuffer.allocate(buffer.capacity() * 2);
            buffer.flip();
            b.put(buffer);
            buffer = b;
        }
    }
}
