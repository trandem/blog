package blog.common.glosory;

import blog.common.concurrent.XFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static blog.common.Utils.cast;

public abstract class ReferenceLifeCycle implements LifeCycle {

    private AtomicInteger reference = new AtomicInteger(0);

    private AtomicReference<Status> status = new AtomicReference<>(Status.STOPPED);

    private XFuture<Void> sFuture = new XFuture<>();


    enum Status {
        STATING, STARTED, STOPPING, STOPPED
    }

    protected abstract void doStart() throws Exception;

    protected abstract long doStop(long timeout, TimeUnit unit) throws Exception;

    @Override
    public boolean start() {
        this.reference.getAndIncrement();
        if (!status.compareAndSet(Status.STOPPED, Status.STATING)) {
            try {
                sFuture.get();
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (ExecutionException e) {
                throw (RuntimeException) e.getCause(); // Cause is runtime
            }
        }

        RuntimeException re = null;
        final long mark = System.nanoTime();
        try {
            doStart();
            final long et = System.nanoTime() - mark;
//            LOGGER.info("lifecyclet: {} started, time: {} nanosecond", this, et);
            return true;
        } catch (Throwable e) {
            throw re = translate(e);
        } finally {
            this.status.set(Status.STARTED);
            if (re != null) sFuture.setThrowable(re);
            else sFuture.setResult(null);
        }
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        //
        if (this.reference.decrementAndGet() != 0) return false;
        if (!this.status.compareAndSet(Status.STARTED, Status.STOPPING)) {
            return false;
        }
        //
        final long mark = System.nanoTime();
        try {
            doStop(timeout, unit);
            final long et = System.nanoTime() - mark;
//            LOGGER.info("lifecyclet: {} stopped, time: {} ms", this, et);
            return true;
        } catch (Throwable e) {
            throw translate(e);
        } finally {
            this.status.set(Status.STOPPED);/* reset the status anyway */
        }
    }

    @Override
    public boolean stop() {
        return stop(0L, TimeUnit.MILLISECONDS);
    }

    /**
     *
     */
    private final RuntimeException translate(final Throwable t) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new RuntimeException("InterruptedException");
        } else {
            return t instanceof RuntimeException ? cast(t) : new RuntimeException(t);/*?*/
        }
    }

    @Override
    public boolean isRunning() {
        return this.status.get() == Status.STARTED;
    }

}
