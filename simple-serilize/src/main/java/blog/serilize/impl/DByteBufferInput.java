package blog.serilize.impl;



import blog.serilize.base.DInput;

import java.nio.ByteBuffer;

public class DByteBufferInput implements DInput {
    private final ByteBuffer buffer;

    public DByteBufferInput(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public DByteBufferInput(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes);
    }


    public int readInt() {
        return buffer.getInt();
    }

    public long readLong() {
        return buffer.getLong();
    }

    public short readShort() {
        return buffer.getShort();
    }

    public byte readByte() {
        return buffer.get();
    }

    public double readDouble() {
        return buffer.getDouble();
    }


    public char readChar() {
        return buffer.getChar();
    }
}
