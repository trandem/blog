package blog.customer.engine.signal;

import blog.customer.engine.pipeline.Shard;

public interface Signal {
    Shard shard();

    void setShard(Shard shard);
}
