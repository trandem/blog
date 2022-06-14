package blog.customer.engine.signal;

import blog.customer.engine.pipeline.Shard;

public class CustomerShardManagerIml implements CustomerShardManager {

    private int numShard;
    private Shard[] shards;

    public CustomerShardManagerIml(int numShard) {
        this.numShard = numShard;
        this.shards = new CustomerShardIml[numShard];
        for (int i =0 ; i < numShard ;  i++){
            this.shards[i] = new CustomerShardIml(i);
        }
    }

    @Override
    public Shard[] getShards() {
        return shards;
    }

    @Override
    public Shard getShards(int id) {
        return shards[id% numShard];
    }

    @Override
    public int getNumShard() {
        return numShard;
    }
}
