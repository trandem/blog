package blog.common.id;

public class Numbers {

    // phan tach so long thanh int va nguoc lai
    public static final long toLong(final int v1, final int v2) {
        return (((long)v1 << 32) & 0xFFFFFFFF00000000L) | ((long)v2 & 0x00000000FFFFFFFFL);
    }

    public static int lowInt(long value) {
        return (int)(value & 0xFFFFFFFFL);
    }

    public static int highInt(final long value) {
        return (int)((value >>> 32) & 0xFFFFFFFFL);
    }


    // phan tach so int thanh short va nguoc lai
    public static final int toInt( short v1, short v2 ) {
        return ((v1 << 16) & 0xFFFF0000) | (v2 & 0x0000FFFF);
    }

    public static short lowShort(int value) {
        return (short)(value & 0xFFFF);
    }

    public static short highShort(final int value) {
        return (short)((value >>> 16) & 0xFFFF);
    }


    // phan tach so short thanh byte va nguoc lai
    public static final short toShort(byte v1, byte v2) {
        return (short)(((v1 << 8) & 0xFF00) | (v2 & 0x00FF));
    }

    public static byte lowByte(short value) {
        return (byte)(value & 0xFF);
    }

    public static byte highByte(short value) {
        return (byte)((value >>> 8) & 0xFF);
    }
}
