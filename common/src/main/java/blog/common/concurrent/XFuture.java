package blog.common.concurrent;

import java.util.concurrent.*;

public class XFuture<T> implements DFuture<T> {

    private volatile T result;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private volatile boolean isDone = false;
    private volatile Throwable throwable;

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return isDone;
    }

    public T get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        if (throwable != null) throw new ExecutionException(throwable);
        return result;
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
        countDownLatch.await(timeout, unit);
        if (result == null){
            throw new TimeoutException("timeout");
        }
        if (throwable != null) throw new ExecutionException(throwable);
        return result;
    }

    public void setResult(T result) {
        this.result = result;
        this.countDownLatch.countDown();
        this.isDone = true;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable; this.countDownLatch.countDown();
    }
}