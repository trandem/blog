package blog.serialize.base;


import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.nio.ByteOrder;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;


public class UnsafeUntil {

    private static final Unsafe UNSAFE = getUnsafe();

    private static final ByteOrder ORDER = ByteOrder.nativeOrder();

    private static final long BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);


    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("can't init unsafe");
        }
    }

    public static int getInt(byte[] buffer, int position) {
        return UNSAFE.getInt(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static byte getByte(byte[] buffer, int position) {
        return UNSAFE.getByte(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static char getChar(byte[] buffer, int position) {
        return UNSAFE.getChar(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static long getLong(byte[] buffer, int position) {
        return UNSAFE.getLong(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static short getShort(byte[] buffer, int position) {
        return UNSAFE.getShort(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static float getFloat(byte[] buffer, int position) {
        return UNSAFE.getFloat(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static double getDouble(byte[] buffer, int position) {
        return UNSAFE.getDouble(buffer, BYTE_ARRAY_OFFSET + position);
    }

    public static void putInt(byte[] buffer, int position, int v) {
        UNSAFE.putInt(buffer, BYTE_ARRAY_OFFSET + position, v);
    }

    public static void putByte(byte[] buffer, int position, byte v) {
        UNSAFE.putByte(buffer, BYTE_ARRAY_OFFSET + position, v);
    }

    public static void putChar(byte[] buffer, int position, char v) {
        UNSAFE.putChar(buffer, BYTE_ARRAY_OFFSET + position, v);
    }

    public static void putLong(byte[] buffer, int position, long v) {
        UNSAFE.putLong(buffer, BYTE_ARRAY_OFFSET + position, v);
    }

    public static void putShort(byte[] buffer, int position, short v) {
        UNSAFE.putShort(buffer, BYTE_ARRAY_OFFSET + position, v);
    }

    public static void putFloat(byte[] buffer, int position, float v) {
        UNSAFE.putFloat(buffer, BYTE_ARRAY_OFFSET + position, v);
    }

    public static void putDouble(byte[] buffer, int position, double v) {
        UNSAFE.putDouble(buffer, BYTE_ARRAY_OFFSET + position, v);
    }


    public static void arraycopy(byte[] sa, int sp, byte[] da, int dp, int n) {
        //
        if(sp + n > sa.length) {
            throw new IllegalArgumentException("sn: " + sa.length + ", sp: " + sp + ", n: " + n);
        } else if(dp + n > da.length) {
            throw new IllegalArgumentException("dn: " + da.length + ", dp: " + dp + ", n: " + n);
        }

        UNSAFE.copyMemory(sa, BYTE_ARRAY_OFFSET + sp, da, BYTE_ARRAY_OFFSET + dp, n); // memcopy is faster?
    }

    public static int getInt(final byte[] a, final int i, final ByteOrder o) {
        int r = UNSAFE.getInt(a, BYTE_ARRAY_OFFSET + i);
        return ORDER == o ? r : Integer.reverseBytes(r);
    }

    public static long getLong(final byte[] a, final int i, final ByteOrder o) {
        long r = UNSAFE.getLong(a, BYTE_ARRAY_OFFSET + i);
        return ORDER == o ? r : Long.reverseBytes(r);
    }

    public static short getShort(final byte[] a, final int i, final ByteOrder o) {
        short r = UNSAFE.getShort(a, BYTE_ARRAY_OFFSET + i);
        return ORDER == o ? r : Short.reverseBytes(r);
    }

    public static char getChar(final byte[] a, final int i, final ByteOrder o) {
        char r = UNSAFE.getChar(a, BYTE_ARRAY_OFFSET + i);
        return ORDER == o ? r : Character.reverseBytes(r);
    }

    public static float getFloat(final byte[] a, final int i, final ByteOrder o) {
        return ORDER == o ? UNSAFE.getFloat(a, BYTE_ARRAY_OFFSET + i) : intBitsToFloat(Integer.reverseBytes(UNSAFE.getInt(a, BYTE_ARRAY_OFFSET + i)));
    }

    public static double getDouble(final byte[] a, final int i, final ByteOrder o) {
        return ORDER == o ? UNSAFE.getDouble(a, BYTE_ARRAY_OFFSET + i) : longBitsToDouble(Long.reverseBytes(UNSAFE.getLong(a, BYTE_ARRAY_OFFSET + i)));
    }

    public static void putInt(byte[] a, int i, ByteOrder o, int v) {
        UNSAFE.putInt(a, BYTE_ARRAY_OFFSET + i, ORDER == o ? v : Integer.reverseBytes(v));
    }

    public static void putLong(byte[] a, int i, ByteOrder o, long v) {
        UNSAFE.putLong(a, BYTE_ARRAY_OFFSET + i, ORDER == o ? v : Long.reverseBytes((v)));
    }

    public static void putShort(byte[] a, int i, ByteOrder o, int v) {
        UNSAFE.putShort(a, BYTE_ARRAY_OFFSET + i, ORDER == o ? (short) v : Short.reverseBytes((short) v));
    }

    public static void putChar(byte[] a, int i, ByteOrder o, int v) {
        UNSAFE.putChar(a, BYTE_ARRAY_OFFSET + i, ORDER == o ? (char) v : Character.reverseBytes((char) v));
    }

    public static void putFloat(final byte[] a, final int i, final ByteOrder o, final float v) {
        if (ORDER == o) {
            UNSAFE.putFloat(a, BYTE_ARRAY_OFFSET + i, (v));
        } else {
            UNSAFE.putInt(a, BYTE_ARRAY_OFFSET + i, Integer.reverseBytes(floatToRawIntBits(v)));
        }
    }

    public static void putDouble(final byte[] a, final int i, final ByteOrder o, final double v) {
        if (ORDER == o) {
            UNSAFE.putDouble(a, BYTE_ARRAY_OFFSET + i, v);
        } else {
            UNSAFE.putLong(a, BYTE_ARRAY_OFFSET + i, Long.reverseBytes(doubleToRawLongBits(v)));
        }
    }
}
