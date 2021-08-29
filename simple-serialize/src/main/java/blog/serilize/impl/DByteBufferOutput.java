package blog.serilize.impl;


import blog.serilize.base.DOutput;

import java.nio.ByteBuffer;


public class DByteBufferOutput implements DOutput {
    private  ByteBuffer buffer;

    public DByteBufferOutput(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public DByteBufferOutput(int size) {
        this.buffer = ByteBuffer.allocate(size);
    }


    public void writeInt(int data) {
        require(4);
        buffer.putInt(data);
    }

    public void writeLong(long data) {
        require(8);
        buffer.putLong(data);
    }

    public void writeShort(short data) {
        require(2);
        buffer.putShort(data);
    }

    public void writeByte(byte data) {
        require(1);
        buffer.put(data);
    }

    public void writeDouble(double data) {
        require(8);
        buffer.putDouble(data);
    }


    private void require(final int n) {
        if(this.buffer.remaining() >= n) return;
        int prev = this.buffer.capacity(), next = prev * 2; while(next - prev < n) next = next * 2;
        ByteBuffer b = ByteBuffer.allocate(next);this.buffer.flip();
        b.put(this.buffer);
        this.buffer = b;
    }

    public void writeChar(char data) {
        buffer.putChar(data);
    }

    public byte[] toArrayBytes() {
        byte[] a = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(a);
        return a;
    }
}
