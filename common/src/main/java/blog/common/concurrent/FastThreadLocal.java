package blog.common.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class FastThreadLocal<T> {
    private static final AtomicInteger MARK = new AtomicInteger(0);

    private final int index = MARK.getAndIncrement();

    private ThreadLocal<T> local;
    private Supplier<T> supplier;

    public FastThreadLocal() {
    }

    public T get(){
        Thread t = Thread.currentThread();
        if (t instanceof DThread){
            DThread.DThreadLocalMap map =((DThread)t).getdThreadLocalMap();
            T value = (T) map.getData(this.index);
            if (value == null && supplier !=null){
                value = supplier.get();
            }
            return value;
        }else {
            if (local == null) local = new ThreadLocal<>();
            return local.get();
        }
    }

    public void set(T value){
        Thread t = Thread.currentThread();
        if (t instanceof DThread){
            DThread.DThreadLocalMap map =((DThread)t).getdThreadLocalMap();
            map.setData(this.index , value);
        }else {
            if (local == null) local = new ThreadLocal<>();
            local.set(value);
        }
    }

    public static <T> FastThreadLocal<T> withInit(Supplier<T> supplier){
        FastThreadLocal<T> instance = new FastThreadLocal<>();
        instance.local = ThreadLocal.withInitial(supplier);
        instance.supplier = supplier;
        return instance;
    }
}
