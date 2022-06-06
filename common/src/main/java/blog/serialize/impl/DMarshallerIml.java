package blog.serialize.impl;

import blog.common.Utils;
import blog.serialize.base.*;
import blog.serialize.base.datatype.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DMarshallerIml implements DMarshaller {
    private final Map<Class<?>, MagicSerialize<?>> registerClass;
    private final Map<Integer, MagicSerialize<?>> registerInt;

    private final AtomicInteger magic = new AtomicInteger(0);

    protected static final byte NIL = (byte) 0x00;
    protected static final byte OID = (byte) 0x10;
    protected static final byte SID = (byte) 0x20;

    protected ThreadLocal<Writer> writerLocal = ThreadLocal.withInitial(Writer::new);
    protected ThreadLocal<Reader> readerLocal = ThreadLocal.withInitial(Reader::new);

    public DMarshallerIml() {
        this.registerClass = new HashMap<>();
        this.registerInt = new HashMap<>();
        registerDefault();
    }

    private void registerDefault() {
        register(new StringSerialize());
        register(new IntergerSerialize());
        register(new HashMapSerialize<>());
        register(new ObjectArraySerialize());
        register(new ListSerialize<>());
    }

    public void register(DSerialize<?> serialize) {
        register(new MagicSerialize<>(magic.getAndIncrement(), serialize));
    }

    private void register(MagicSerialize<?> serialize) {
        registerClass.put(serialize.getClasses(), serialize);
        registerInt.put(serialize.getMagic(), serialize);
    }

    public <T extends DMarshallable> void register(Class<T> c, DInstance<T> ins) {
        register(new MagicSerialize<>(magic.getAndIncrement(), new DObjectSerialize<>(ins, c)));
    }

    public <T extends DMarshallable> void register(DInstance<T> ins) {
        Class<?> aClass = detectActualType(ins);
        if (aClass == null) {
            throw new RuntimeException("Object need implement DInstance");
        }
        register(new MagicSerialize<>(magic.getAndIncrement(), new DObjectSerialize<>(ins, aClass)));
    }


    public void write(Object data, DOutput output) {
        Writer writer = writerLocal.get();
        writer.acquire();
        try {
            if (data == null) {
                output.writeByte(NIL);
                return;
            }
            int cacheIndex = writer.registerObject(data);
            if (cacheIndex >= 0) {
                output.writeByte(OID);

                output.writeIntOptimise(cacheIndex);
            }
            MagicSerialize<Object> ms = Utils.cast(registerClass.get(data.getClass()));
            if (ms != null) {
                output.writeByte(SID);
                output.writeIntOptimise(ms.getMagic());
                final DSerialize<Object> s = ms.getSerialize();
                s.write(this, output, data);
            } else {
                throw new RuntimeException("Object need register first " + data.getClass());
            }
        } finally {
            writer.release();
        }

    }

    public <T> T read(DInput input) {
        Reader reader = readerLocal.get();
        reader.acquire();
        try {
            switch (input.readByte()) {
                case NIL:
                    return null;
                case SID:
                    int sid = input.readIntPositiveOptimise();
                    int index = reader.referrences++;
                    MagicSerialize<Object> numberSerialize = Utils.cast(registerInt.get(sid));
                    final Object r1 = numberSerialize.getSerialize().read(this, input);
                    reader.cacheObject.put(index, r1);
                    return Utils.cast(r1);
                case OID:
                    final int oid = input.readIntPositiveOptimise();
                    return Utils.cast(reader.cacheObject.get(oid));
                default:
                    throw new RuntimeException("unknown marshaller");
            }
        } finally {
            reader.release();
        }
    }

    private static <V extends DInstance<?>> Class<?> detectActualType(V test) {
        Type[] types = test.getClass().getGenericInterfaces();
        for (Type type : types) {
            if (((ParameterizedType) types[0]).getRawType() == DInstance.class) {
                return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
            }
        }
        return null;
    }

    protected static class Reader {
        private final Map<Integer, Object> cacheObject = new HashMap<>();
        protected int depth, referrences;

        public void acquire() {
            ++this.depth;
        }

        public final void release() {
            if (--this.depth == 0) reset();
        }

        public void reset() {
            this.depth = 0;
            this.referrences = 0;
            cacheObject.clear();
        }
    }

    protected static class Writer {
        private final Map<Object, Integer> cacheObject = new HashMap<>();
        private int depth;

        public int registerObject(Object object) {
            int index = cacheObject.size();
            Integer cacheIndex = cacheObject.putIfAbsent(object, index);
            return cacheIndex == null ? -index - 1 : cacheIndex;
        }

        public void acquire() {
            ++this.depth;
        }

        public final void release() {
            if (--this.depth == 0) reset();
        }

        public void reset() {
            this.depth = 0;
            cacheObject.clear();
        }
    }
}
