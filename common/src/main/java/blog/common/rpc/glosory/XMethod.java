package blog.common.rpc.glosory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class XMethod {
    private Method method;
    private Object target;

    private int signature;

    public XMethod(Method method, Object target) {
        this.method = method;
        this.target = target;
        this.signature = signature(method);
    }

    public  static int signature(Method method) {
        int sig = method.getName().hashCode();
        for (Class<?> x : method.getParameterTypes()) {
            sig += x.getName().hashCode();
        }
        return sig;
    }

    public int getSignature() {
        return signature;
    }

    public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return this.method.invoke(target, args);
    }
}
