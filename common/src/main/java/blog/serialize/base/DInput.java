package blog.serialize.base;

public interface DInput {

    int readInt();

    default int readIntPositiveOptimise() {
        int output = 0;
        for (int i = 0; i < 32; i += 7) {
            int b = readByte() & 0xff;
            output |= ((b & 0x7F) << i);
            if ((b & 0x80) == 0) {
                return output;
            }
        }
        throw new RuntimeException("can't read int: " + Integer.toBinaryString(output));
    }

    long readLong();


    default long readLongPositiveOptimise() {
        long output = 0;
        for (int i = 0; i < 64; i += 7) {
            int b = readByte() & 0xff;
            output |= ((b & 0x7FL) << i);
            if ((b & 0x80) == 0) {
                return output;
            }
        }
        throw new RuntimeException("can't read int: " + Long.toBinaryString(output));
    }

    short readShort();

    byte readByte();

    double readDouble();

    byte[] readBytes();

    default String readString() {
        StringBuilder sb = new StringBuilder();
        int size = readIntPositiveOptimise();
        for (int i = 0; i < size; i++) {
            sb.append((char)readIntPositiveOptimise());
        }
        return sb.toString();
    }

    char readChar();
}
