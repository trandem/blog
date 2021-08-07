package blog.rpc.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class XFuture<T> implements Future<T> {

    private volatile T result;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean isDone = false;

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return isDone;
    }

    public T get() throws InterruptedException {
        countDownLatch.await();
        return result;
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        countDownLatch.await(timeout, unit);
        if (result == null){
            throw new TimeoutException("timeout");
        }
        return result;
    }

    public void setResult(T result) {
        this.result = result;
        this.countDownLatch.countDown();
        this.isDone = true;
    }
}