package tutorial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Test {
    protected final ReentrantLock lock = new ReentrantLock(false);
    protected final Condition notEmpty = this.lock.newCondition();
    private boolean isDone = false;

    public void waitTimeout() throws InterruptedException {
        try {
            lock.lock();
            long ttl = TimeUnit.MILLISECONDS.toNanos(3000);
            while (!isDone) {
                ttl = notEmpty.awaitNanos(ttl);
//                System.out.println(ttl);
                if (ttl <= 0) break;
            }
//            System.out.println("getlock again");
        } finally {
            lock.unlock();
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        RandomAccessFile randomAccess = new RandomAccessFile(new File("dev"),"rwd");
//        randomAccess.writeInt(4);
//        randomAccess.writeInt(4);
//        randomAccess.writeInt(4);
//        randomAccess.close();
        ByteBuffer buffer = ByteBuffer.allocate(100);
        int r = randomAccess.getChannel().read(buffer);
        System.out.println(r);
        buffer.getInt();
        buffer.getInt();
        buffer.compact();
        r = randomAccess.getChannel().read(buffer);
        System.out.println(r);


    }
}
