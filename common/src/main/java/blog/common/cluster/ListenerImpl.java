package blog.common.cluster;

import blog.common.DReflections;
import blog.common.cluster.glosory.Listener;
import blog.common.cluster.glosory.MethodListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ListenerImpl implements Listener {
    private String topic;
    private String type;
    private Object target;
    private Method method;


    public ListenerImpl(String topic, String type, Object target, Method method) {
        this.topic = topic;
        this.type = type;
        this.target = target;
        this.method = method;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListenerImpl listener = (ListenerImpl) o;
        return Objects.equals(topic, listener.topic) &&
                Objects.equals(type, listener.type) &&
                Objects.equals(target, listener.target) &&
                Objects.equals(method, listener.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, type, target, method);
    }

    public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }

    public static class Factory implements Listener.Factory{

        public List<Listener> create(Object target) {

            List<Listener> listeners = new ArrayList<>();
            Set<Method> methodSet = DReflections.findMethod(target, MethodListener.class);
            for (Method method : methodSet) {
                MethodListener methodListener = method.getAnnotation(MethodListener.class);
                ListenerImpl listener = new ListenerImpl(methodListener.topic(), methodListener.type(), target, method);
                listeners.add(listener);
            }

            return listeners;
        }
    }
}
