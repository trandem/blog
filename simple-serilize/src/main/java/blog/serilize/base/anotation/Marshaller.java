package blog.serilize.base.anotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Marshaller {
    Class<?> name() default Object.class;
    int number() default -1;
}
