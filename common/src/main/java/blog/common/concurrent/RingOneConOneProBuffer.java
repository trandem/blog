package blog.common.concurrent;


import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
/**
 * this buffer use for one consumer, one producer
 */
public class RingOneConOneProBuffer<T> {
    protected T[] buffer;
    protected int readIndex;
    protected int writeIndex;
    protected final int capacity;
    protected AtomicInteger size;

    public RingOneConOneProBuffer() {
        this(10000);
    }

    public RingOneConOneProBuffer(int k) {
        this.buffer = (T[]) new Object[k];
        this.capacity = k;
        this.readIndex = 0;
        this.writeIndex = 0;
        this.size = new AtomicInteger(0);
    }

    public synchronized boolean offer(T value) {
        if (isFull()) return false;
        buffer[writeIndex] = value;
        writeIndex++;
        if (writeIndex == capacity) writeIndex -= capacity;
        size.getAndIncrement();
        return true;
    }

    public T poll() {
        if (isEmpty()) return null;
        int index = readIndex;
        T x = buffer[index];
        readIndex++;
        if (readIndex == capacity) readIndex -= capacity;
        size.getAndDecrement();
        return x;
    }

    public boolean isEmpty() {
        return size.get() == 0;
    }

    public boolean isFull() {
        return size.get() == capacity;
    }
}
