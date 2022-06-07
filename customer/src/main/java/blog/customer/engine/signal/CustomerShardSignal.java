package blog.customer.engine.signal;

import blog.customer.engine.request.CustomerRequest;

public class CustomerShardSignal implements RequestSignal {

    private int shard;
    private CustomerRequest request;


    public CustomerShardSignal() {
    }

    public CustomerShardSignal(int shard, CustomerRequest request) {
        this.shard = shard;
        this.request = request;
    }

    @Override
    public int shard() {
        return shard;
    }

    @Override
    public void setShard(int shard) {
        this.shard = shard;
    }

    @Override
    public CustomerRequest getRequest() {
        return request;
    }
}
