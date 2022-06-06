package blog.customer.engine.pipeline;

public interface PipeLine<T> {

    boolean submit(T t);
}
