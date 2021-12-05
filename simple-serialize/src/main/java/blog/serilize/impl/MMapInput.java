package blog.serilize.impl;

import blog.serilize.base.DInput;

import java.nio.MappedByteBuffer;

public class MMapInput implements DInput {

    private MappedByteBuffer buffer;

    public MMapInput(MappedByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int readInt() {
        return buffer.getInt();
    }

    @Override
    public long readLong() {
        return buffer.getLong();
    }

    @Override
    public short readShort() {
        return buffer.getShort();
    }

    @Override
    public byte readByte() {
        return buffer.get();
    }

    @Override
    public double readDouble() {
        return buffer.getDouble();
    }

    public void reset(int position){
        this.buffer.position(position);
    }

    public int mark(){
        return this.buffer.position();
    }

    @Override
    public char readChar() {
        return buffer.getChar();
    }
}
