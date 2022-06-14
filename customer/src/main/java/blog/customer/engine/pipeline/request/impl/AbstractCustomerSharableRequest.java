package blog.customer.engine.pipeline.request.impl;

import blog.customer.engine.pipeline.request.CustomerSharableRequest;
import blog.customer.storage.model.po.CustomerPo;

public abstract class AbstractCustomerSharableRequest implements CustomerSharableRequest {
    protected int customerId;
    protected CustomerPo customerPo;
    protected long id;


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
