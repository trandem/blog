package blog.common.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ListenableFuture<T> implements DFuture<T> {

    protected volatile T result;
    protected volatile Throwable throwable;
    protected volatile DFuture.Listener<T> listener;
    private volatile boolean isDone = false;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    public void setListener(Listener<T> listener) {
        this.listener = listener;
        if (isDone()) {
            listener.onComplete(this);
        }
    }

    @Override
    public T get() throws InterruptedException {
        countDownLatch.await();
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        countDownLatch.await(timeout, unit);
        if (result == null){
            throw new TimeoutException("timeout");
        }
        return result;
    }

    @Override
    public void setResult(T result) {
        this.result = result;
        this.isDone = true;
        this.countDownLatch.countDown();
        if (listener != null) listener.onComplete(this);
    }

    @Override
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
        this.isDone = true;
        this.countDownLatch.countDown();
        if (listener != null) listener.onComplete(this);
    }
}
