package blog.serilize.base.anotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Marshaller {
    String name() default "";
    int number() default -1;
}
