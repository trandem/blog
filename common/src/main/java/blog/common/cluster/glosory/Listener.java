package blog.common.cluster.glosory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface Listener {

    Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException;

    String getTopic();

    String getType();

    interface Factory {
        List<Listener> create(Object target);
    }

}
