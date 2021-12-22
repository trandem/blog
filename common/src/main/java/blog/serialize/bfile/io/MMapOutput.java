package blog.serialize.bfile.io;

import blog.serialize.base.DOutput;
import blog.serialize.bfile.store.MMapFileStore;

import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;

public class MMapOutput implements BFileOutput {

    private final MappedByteBuffer buffer;
    private final MMapFileStore store;

    public MMapOutput(MMapFileStore store) {
        this.buffer = store.getBuffer();
        this.store = store;
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
        writeIntOptimise(data.length);
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

    public void close() {
        this.buffer.force();
    }


    public void reset(int position) {
        this.buffer.position(position);
    }

    public int mark() {
        return this.buffer.position();
    }

    @Override
    public void flush() {
        //todo nothing
//        buffer.force();
    }

    @Override
    public byte[] toArrayBytes() {
        return new byte[0];
    }

    private void require(final int n) {
        if (this.buffer.position() + n > store.getCapacity()) throw new BufferOverflowException();
    }
}
