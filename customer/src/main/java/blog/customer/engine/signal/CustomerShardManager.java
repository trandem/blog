package blog.customer.engine.signal;

import blog.customer.engine.pipeline.Shard;

public interface CustomerShardManager {

    Shard[] getShards();

    Shard getShards(int id);

    int getNumShard();
}
