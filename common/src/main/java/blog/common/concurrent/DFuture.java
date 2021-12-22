package blog.common.concurrent;

import java.util.concurrent.Future;

public interface DFuture<T> extends Future<T> {

    public void setResult(T result);

    public void setThrowable(Throwable throwable);

    interface Listener<T> {
        void onComplete(DFuture<T> t);
    }
}
