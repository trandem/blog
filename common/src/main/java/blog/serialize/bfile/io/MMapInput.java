package blog.serialize.bfile.io;

import java.nio.MappedByteBuffer;

public class MMapInput implements BFileInput {

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

    @Override
    public byte[] readBytes() {
        int size = readIntPositiveOptimise();
        byte[] bytes = new byte[size];
        buffer.get(bytes, 0, bytes.length);
        return bytes;
    }

    @Override
    public void close() {

    }

    public void reset(int position) {
        this.buffer.position(position);
    }

    public int mark() {
        return this.buffer.position();
    }

    @Override
    public void skipBytes(int numBytes) {
        for (int i =0 ; i< numBytes;i++){
            readByte();
        }
    }

    @Override
    public char readChar() {
        return buffer.getChar();
    }
}
