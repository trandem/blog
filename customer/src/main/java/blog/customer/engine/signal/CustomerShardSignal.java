package blog.customer.engine.signal;

import blog.common.concurrent.DFuture;
import blog.customer.engine.pipeline.RequestSignalStep;
import blog.customer.engine.pipeline.Shard;
import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponse;

public class CustomerShardSignal implements RequestSignal {

    private Shard shard;
    private CustomerRequest request;

    protected DFuture<CustomerResponse> future;

    @Override
    public DFuture<CustomerResponse> getCustomerFuture() {
        return future;
    }

    @Override
    public void setCustomerFuture(DFuture<CustomerResponse> customerFuture) {
        this.future = customerFuture;
    }
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
