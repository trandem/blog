package blog.common.concurrent;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

public class Concurrents {

    public static <T> T read(StampedLock lock, Supplier<T> task) {
        long v1 = lock.tryOptimisticRead(); T r = task.get(); if (lock.validate(v1)) return r;
        long v2 = lock.readLock(); try { return task.get(); } finally { lock.unlockRead(v2); }
    }

}
