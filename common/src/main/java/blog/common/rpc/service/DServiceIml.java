package blog.common.rpc.service;

import blog.common.rpc.glosory.DService;
import blog.common.rpc.glosory.XMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DServiceIml implements DService {

    private final int id;
    private final int serverId;
    private final String domain;

    private final Map<Integer, XMethod> methodMap = new HashMap<>();

    public DServiceIml(int id, int serverId, String name, Object target) {
        this.id = id;
        this.serverId = serverId;
        this.domain = name;

        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            XMethod xMethod = new XMethod(method, target);
            methodMap.put(xMethod.getSignature(), xMethod);
        }

    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getServerId() {
        return this.serverId;
    }

    @Override
    public String getDomain() {
        return this.domain;
    }

    @Override
    public Object invoke(int methodSignal, Object[] args) throws Throwable {
        if (methodSignal == 0) return null;
        return methodMap.get(methodSignal).invoke(args);
    }

    public static class ServiceFactory implements DService.Factory {
        AtomicInteger serviceId = new AtomicInteger(29);

        @Override
        public DService create(int serverId, String name, Object target) {
            return new DServiceIml(serviceId.getAndIncrement(), serverId, name, target);
        }
    }
}
