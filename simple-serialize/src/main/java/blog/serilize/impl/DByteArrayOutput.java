package blog.serilize.impl;

import blog.serilize.base.DOutput;
import blog.serilize.base.UnsafeUntil;

public class DByteArrayOutput implements DOutput {

    private byte[] buffer;
    private int position;

    public DByteArrayOutput(int size) {
        this.buffer = new byte[size];
        this.position = 0;
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

    private void require(final int n) {
        if (this.buffer.length - position < n) {
            int newSize = buffer.length * 2;
            while (newSize - position < n) {
                newSize *= 2;
            }
            byte[] temp = new byte[newSize];
            System.arraycopy(buffer, 0, temp, 0, buffer.length);
            this.buffer = temp;
        }
    }
}
