package blog.serilize.impl;

import blog.serilize.base.DInput;
import blog.serilize.base.UnsafeUntil;

public class DByteArrayInput implements DInput {
    private byte[] buffer;
    private int position;

    public DByteArrayInput(byte[] buffer) {
        this.buffer = buffer;
        position = 0;
    }


    @Override
    public int readInt() {
        int data = UnsafeUntil.getInt(buffer, position);
        position += 4;
        return data;
    }

    @Override
    public long readLong() {
        long data = UnsafeUntil.getLong(buffer, position);
        position += 8;
        return data;
    }

    @Override
    public short readShort() {
        short data = UnsafeUntil.getShort(buffer, position);
        position += 2;
        return data;
    }

    @Override
    public byte readByte() {
        byte data = UnsafeUntil.getByte(buffer,position);
        position += 1;
        return data;
    }

    @Override
    public double readDouble() {
        double data = UnsafeUntil.getDouble(buffer,position);
        position += 8;
        return data;
    }

    @Override
    public char readChar() {
        char data = UnsafeUntil.getChar(buffer,position);
        position += 2;
        return data;
    }
}
