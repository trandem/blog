package blog.customer.engine.signal;

import blog.customer.engine.pipeline.Shard;

import java.util.Objects;

public class CustomerShardIml implements Shard {
    private int shardId;


    @Override
    public int getId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerShardIml that = (CustomerShardIml) o;
        return shardId == that.shardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shardId);
    }
}
