package blog.customer.engine.pipeline;

import blog.common.glosory.LifeCycle;

public interface RequestSignalStep<T> extends LifeCycle {

    Shard getShard();

    void process(T[] signals, int count);

    int getBatch();

    interface factory<T> {
        RequestSignalStep<T> create(Shard shard);
    }
}
