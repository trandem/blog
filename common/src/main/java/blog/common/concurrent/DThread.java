package blog.common.concurrent;

import java.util.Arrays;

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

    public static void main(String[] args) {
        FastThreadLocal<String> x = new FastThreadLocal<>();

        DThread dThread = new DThread( new Runnable() {
            @Override
            public void run() {
                FastThreadLocal<Integer> y = FastThreadLocal.withInit(() -> 0);
//                y.set(10);
                System.out.println(y.get());
            }
        },"test");

        dThread.start();
        System.out.println("lol");
    }
}
