package blog.common.rpc.client;

import blog.common.id.IdGenerator;
import blog.common.rpc.glosory.ProxyFactory;
import blog.common.rpc.glosory.RpcRequest;
import blog.common.rpc.glosory.XMethod;
import blog.common.rpc.service.Simple;
import blog.serialize.base.DMarshaller;
import blog.serialize.impl.AllMarshaller;
import blog.serialize.impl.DMarshallers;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class SimpleCliProxy {

    private DRpcClientImpl client;
    private DMarshaller marshaller = AllMarshaller.DEFAULT;
    private final Map<String,Object> clientName = new HashMap<>();

    private ProxyFactory factory = new CglibFactory();


    public SimpleCliProxy(DRpcClientImpl client){
        this.client = client;
        client.start();
    }

    private ClientHandler createHandler(String name){
        return new ClientHandler(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getClient(Class<T> tClass, String name){
        if (clientName.containsKey(name)){
            return (T)clientName.get(name);
        }

        ClientHandler handler = createHandler(name);
        T t = factory.getProxy(tClass,handler);
        clientName.put(name,t);
        return t;
    }

    public class ClientHandler implements InvocationHandler{

        public ClientHandler(String serviceName) {
            this.serviceName = serviceName;
        }

        private final String serviceName;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            RpcRequest request = new RpcRequest();
            request.setBusId(Integer.parseInt(System.getProperty("APP_ID")));
            request.setRequestId(IdGenerator.instance.nextId());
            request.setServiceName(serviceName);
            request.setMethodSig(XMethod.signature(method));
            request.setDomain("demo");
            request.setArgs(DMarshallers.marshaller(args, marshaller));
            return client.sendRequest(request);
        }
    }


    public static class Factory implements ProxyFactory {

        public <T> T getProxy(Class<T> clazz, InvocationHandler handler) {
            final ClassLoader cl = clazz.getClassLoader();
            final Class<?> classes[] = new Class<?>[]{clazz};
            return (T) Proxy.newProxyInstance(cl, classes, handler);
        }
    }

    public static class CglibFactory implements  ProxyFactory{

        public <T> T getProxy(Class<T> clazz, InvocationHandler handler) {
            final Enhancer enhancer = new Enhancer();
            enhancer.setInterfaces(new Class<?>[]{clazz});
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
                        throws Throwable {
                    return handler.invoke(obj, method, args);
                }
            });
            return (T)enhancer.create();
        }

    }

    public void setClient(DRpcClientImpl client) {
        this.client = client;
    }

    public void setMarshaller(DMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    public void setFactory(ProxyFactory factory) {
        this.factory = factory;
    }

    public static void main(String[] args) {

        DRpcClientImpl client = new DRpcClientImpl();
        SimpleCliProxy proxy = new SimpleCliProxy(client);

        Simple simple = proxy.getClient(Simple.class,"simple");

        System.out.println(simple.add(4,6));
//        System.out.println(simple.showServerConfig());
//        System.out.println(simple.getStartEvent());
//        System.out.println(simple.add(4,6));
//        System.out.println(simple.add(2,6));
    }
}
