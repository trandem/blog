package blog.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class DReflections {

    public static Set<Method> findMethod(Object target, Class<? extends Annotation> annotation) {
        Set<Method> methodSet = new HashSet<>();

        List<Class<?>> classList = new ArrayList<>();
        Class<?> aClass = target.getClass();
        while (!aClass.equals(Object.class)) {
            classList.add(aClass);
            Collections.addAll(classList, aClass.getInterfaces());
            aClass = aClass.getSuperclass();
        }

        for (Class<?> findClass : classList) {
            for (Method method : findClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) methodSet.add(method);
            }
        }

        return methodSet;
    }

}
