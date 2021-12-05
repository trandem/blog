package blog.serilize.base;

import blog.serilize.impl.DByteArrayOutput;

public interface DOutput {
    void writeInt(int data);

    default void writeIntOptimise(int value) {
        if ((value < 0)) throw new IllegalArgumentException("pack int: " + value);
        int x;
        while (true) {
            x = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                this.writeByte((byte) (x | 0x80));
            } else {
                this.writeByte((byte) x);
                break;
            }
        }
    }

    public DOutput clear();
    default void writeLongOptimise(int value) {
        if (value < 0L) throw new IllegalArgumentException("pack long: " + value);
        long x;
        while (true) {
            x = value & 0x7FL;
            value >>>= 7;
            if (value != 0L) {
                writeByte((byte) (x | 0x80L));
            } else {
                writeByte((byte) x);
                break;
            }
        }
    }

    void writeLong(long data);

    void writeShort(short data);

    void writeByte(byte data);

    void writeDouble(double data);

    default void writeString(String data) {
        int size = data.length();
        writeIntOptimise(size);
        for (int i = 0; i < size; i++) {
            writeIntOptimise(data.charAt(i));
        }
    }

    void writeChar(char data);

    byte[] toArrayBytes();
}
