package blog.customer.engine.pipeline.request.impl;

import blog.customer.engine.pipeline.request.CustomerSharableRequest;

public abstract class AbstractCustomerSharableRequest implements CustomerSharableRequest {
    private int customerId;

    private long id;

    public AbstractCustomerSharableRequest(int customerId, long id) {
        this.customerId = customerId;
        this.id = id;
    }

    @Override
    public int getCustomerId() {
        return customerId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }
}
