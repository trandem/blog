package blog.serialize.impl.io;



import blog.serialize.base.DInput;

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

    @Override
    public byte[] readBytes() {
        int size = readIntPositiveOptimise();
        byte[] bytes = new byte[size];
        buffer.get(bytes, 0, bytes.length);
        return bytes;
    }


    public char readChar() {
        return buffer.getChar();
    }
}
