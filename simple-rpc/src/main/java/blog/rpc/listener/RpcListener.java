package blog.rpc.listener;

import blog.rpc.support.XFuture;

public interface RpcListener<T> {
    XFuture<T> onMessage(T mess);
}
