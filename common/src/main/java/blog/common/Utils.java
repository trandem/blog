package blog.common;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

public class Utils {
    public static <T> T cast(Object x) {
        return (T) x;
    }

    public static boolean disposeDirectByteBuf (ByteBuffer buffer) {
        if(!(buffer instanceof DirectBuffer)) return false;
        Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
        if(cleaner == null) { return false; /* ? */ } else cleaner.clean(); return true;
    }
}
