package blog.customer.engine.signal;

import blog.customer.engine.pipeline.Shard;
import blog.customer.engine.pipeline.request.CustomerRequest;

public class CustomerShardSignal implements RequestSignal {

    private Shard shard;
    private CustomerRequest request;


    public CustomerShardSignal() {
    }

    public CustomerShardSignal(Shard shard, CustomerRequest request) {
        this.shard = shard;
        this.request = request;
    }

    @Override
    public Shard shard() {
        return shard;
    }

    @Override
    public void setShard(Shard shard) {
        this.shard = shard;
    }

    @Override
    public CustomerRequest getRequest() {
        return request;
    }
}
