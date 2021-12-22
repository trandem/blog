package blog.serialize.impl.io;

import blog.serialize.base.DOutput;
import blog.serialize.base.UnsafeUntil;

public class DByteArrayOutput implements DOutput {

    private byte[] buffer;
    private int position;
    private final int max;

    public DByteArrayOutput(int min, int max) {
        this.max = max;
        if (min > max) throw new RuntimeException("min > max");
        this.position = 0;
        this.buffer = new byte[min];
    }

    public DByteArrayOutput() {
        this(1024, 1024);
    }

    @Override
    public void writeInt(int data) {
        require(4);
        UnsafeUntil.putInt(buffer, position, data);
        position += 4;
    }

    @Override
    public void writeLong(long data) {
        require(8);
        UnsafeUntil.putLong(buffer, position, data);
        position += 8;
    }

    @Override
    public void writeShort(short data) {
        require(2);
        UnsafeUntil.putShort(buffer, position, data);
        position += 2;
    }

    @Override
    public void writeByte(byte data) {
        require(1);
        UnsafeUntil.putByte(buffer, position, data);
        position += 1;
    }

    @Override
    public void writeBytes(byte[] data) {
        if (data ==null) {
            writeIntOptimise(0);
            return;
        }
        writeIntOptimise(data.length);
        require(data.length);
        UnsafeUntil.arraycopy(data, 0, buffer, position, data.length);
        position += data.length;
    }

    @Override
    public void writeDouble(double data) {
        UnsafeUntil.putDouble(buffer, position, data);
        require(8);
        position += 8;
    }

    @Override
    public void writeChar(char data) {
        UnsafeUntil.putChar(buffer, position, data);
        require(2);
        position += 2;
    }

    @Override
    public byte[] toArrayBytes() {
        byte[] output = new byte[position];
        System.arraycopy(buffer, 0, output, 0, position);
        return output;
    }

    public DByteArrayOutput clear() {
        this.position = (0);
        return this;
    }


    private void require(final int n) {
        if (n > max) throw new RuntimeException("buffer over flow");

        if (this.buffer.length - position < n) {
            int newSize = buffer.length * 2;
            while (newSize - position < n) {
                newSize *= 2;
            }
            if (newSize > max) throw new RuntimeException("buffer over flow");
            byte[] temp = new byte[newSize];
            System.arraycopy(buffer, 0, temp, 0, buffer.length);
            this.buffer = temp;
        }
    }
}
