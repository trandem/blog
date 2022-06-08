package blog.customer.engine.pipeline;

import blog.common.glosory.LifeCycle;

public interface PipeLine<T> extends LifeCycle {

    boolean submit(T t);
}
