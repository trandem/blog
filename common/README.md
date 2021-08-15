#Custom ThreadLocal Java with High READ/WRITE performance

**ThreadLocal** là một công cụ rất mạnh mẽ của **Java Concurrent**. Nó cung cấp API để lưu data trên từng **Thread**, các **Thread** tự quản lý data của mình. Khi cần dùng
thì không cần phải khởi tạo lại dữ liệu mà có thể lấy ra dùng trực tiếp mà không cần khởi tạo lại đối tượng từ đó giúp tiết  kiệm thời gian.

Chúng ta thường hay sử dụng các loại API sau :
- `public T get()` : dùng để lấy dữ liệu lưu trong **Thread**.
- `public void set(T value)` : dùng để lưu dữ liệu vào **Thread**.

Tuy nhiên sau khi đọc code của Java tôi nhận thấy **ThreadLocal** không tối ưu cho 2 loại **API** này. Cụ thể tôi đã search trên mạng và thấy một bài khá hay về 
**FastThreadLocal** của [netty](https://programmer.ink/think/why-netty-s-fastthreadlocal-is-fast.html).

## Phân tích API của ThreadLocal.
Các bạn tham khảo tại [link](https://programmer.ink/think/why-netty-s-fastthreadlocal-is-fast.html) sau. Dưới đây tôi xin giải thích đơn giản như sau.

Đầu tiên hãy phân tích hàm **set** của **Java**.
```java

        public void set(T value) {
                Thread t = Thread.currentThread();
                ThreadLocalMap map = getMap(t);
                if (map != null)
                    map.set(this, value);
                else
                    createMap(t, value);
        }
            
        ThreadLocalMap getMap(Thread t) {
            return t.threadLocals;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }
        /**
         * Increment i modulo len.
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

```

Như thường lệ thì **Java** sẽ sử dụng **Array** để lưu giá trị của một **Map**. Khác với **HashMap** sử dụng **LinkedList** hoặc **BTree** để lưu giá trị của Key
khi bị trùng **HashCode** để đảm bảo được khi lấy dữ liệu ra sẽ được **O(1)**. Tại **ThreadLocal** với implement bên trên ta thấy như sau:
- Khi 2 key không trùng mã **HashCode** thì các key được lưu tại vị trí `int i = key.threadLocalHashCode & (len-1);` điều này đảm bảo được việc set,get đạt độ phức tạp **O(1)**
- Khi 2 key trùng mã **HashCode** thì sẽ tìm vị trí liền kề tiếp theo trong table mà tại đó giá trị bằng null, sau đó gán value vào vị trí đó. Điều này dẫn đến khi ta dùng
phương thức get,set không còn đạt được độ phức tạp **O(1)** nữa. Điều này sẽ dẫn đến key tiếp theo của bạn đã bị 1 key khác không trùng mã hashcode dữ vị trí đó.

Tiếp theo ta đến phương thức get của **ThreadLocal**.

```java

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }
            /**
             * Version of getEntry method for use when key is not found in
             * its direct hash slot.
             *
             * @param  key the thread local object
             * @param  i the table index for key's hash code
             * @param  e the entry at table[i]
             * @return the entry associated with key, or null if no such
             */
            private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
                Entry[] tab = table;
                int len = tab.length;
    
                while (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == key)
                        return e;
                    if (k == null)
                        expungeStaleEntry(i);
                    else
                        i = nextIndex(i, len);
                    e = tab[i];
                }
                return null;
            }
```

**ThreadLocal** sẽ tìm rất nhanh nếu không bị trùng **HashCode** nhưng vấn đề sẽ tăng lên khi chúng ta bị trùng mã **HashCode**, khi đó **ThreadLocal** sẽ phải duyệt qua 1 lượt 
các phần tử lưu trong **table** để tìm ra key,value chính xác điều này sẽ mất rất nhiều thời gian. Vì vấn đề này **netty** đã xây dựng 1 class **FastThreadLocal** riêng nhằm 
tối ưu phương pháp get,set bạn có thể tìm tại [blog](https://programmer.ink/think/why-netty-s-fastthreadlocal-is-fast.html) 

## Custom FastThreadLocal
Để dùng **FastThreadLocal** của netty ta phải import thư viện **netty** vào project điều đó có thể gây lãng phí vì vậy tại đây tôi sẽ dựa trên ý tưởng của **netty** xây dụng 
ra một **FastThreadLocal** hy vọng sẽ giúp ích cho project của các bạn.

Đê xây một FastThreadLocal chúng ta cần phải xây dụng 2 thứ sau :
- `DThread` một Thread kế thừa Thread của Java nhưng sẽ chứa thêm DThreadLocalMap để lưu giá trị của Thread thay vì `ThreadLocal.ThreadLocalMap` của Java
- `FastThreadLocal<T>` để quản lý API get,set.

Các phương thức get,set
```java
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

public class DThread extends Thread {

    private final DThreadLocalMap dThreadLocalMap;

    public DThread(Runnable runnable, String s) {
        super(runnable, s);
        this.dThreadLocalMap = new DThreadLocalMap();
    }

    public DThread(Runnable runnable) {
        super(runnable);
        this.dThreadLocalMap = new DThreadLocalMap();
    }

    public DThread() {
        this.dThreadLocalMap = new DThreadLocalMap();
    }

    public DThreadLocalMap getdThreadLocalMap() {
        return dThreadLocalMap;
    }

    public static class DThreadLocalMap {
        private static final int INIT_SIZE = 8;
        private Object[] data;

        public DThreadLocalMap() {
            this.data = new Object[INIT_SIZE];
        }

        public Object getData(int index) {
            if (index > data.length) return null;
            return data[index];
        }

        private void expand(){
            Object[] oldArray = data;

            // copy fom netty
            int newCapacity = oldArray.length;
            newCapacity |= newCapacity >>>  1;
            newCapacity |= newCapacity >>>  2;
            newCapacity |= newCapacity >>>  4;
            newCapacity |= newCapacity >>>  8;
            newCapacity |= newCapacity >>> 16;
            newCapacity ++;

            data = Arrays.copyOf(oldArray, newCapacity);
        }

        public void setData(int index, Object value) {
            if (index > data.length){
                expand();
            }

            Object[] temp = data;
            temp[index] = value;

        }
    }
}

```

Các bạn có thể tham khảo cách **code** này hoặc cải tiến lên để phù hợp với bài toán của mình. Chắc chắn cách implement này sẽ có độ phức tạp O(1).
