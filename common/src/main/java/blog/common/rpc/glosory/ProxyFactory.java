package blog.common.rpc.glosory;

import java.lang.reflect.InvocationHandler;

public interface ProxyFactory {
    <T> T getProxy(Class<T> clazz, InvocationHandler handler);
}
