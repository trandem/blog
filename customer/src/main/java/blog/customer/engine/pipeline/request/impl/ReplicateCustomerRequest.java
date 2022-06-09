package blog.customer.engine.pipeline.request.impl;

import blog.customer.engine.pipeline.request.CustomerRequestType;
import blog.customer.storage.model.po.CustomerPo;

public class ReplicateCustomerRequest extends AbstractCustomerSharableRequest {

    private CustomerPo customerPo;

    public ReplicateCustomerRequest(int customerId, long id, CustomerPo customerPo) {
        super(customerId, id);
        this.customerPo = customerPo;
    }


    public CustomerPo getCustomerPo() {
        return customerPo;
    }

    public void setCustomerPo(CustomerPo customerPo) {
        this.customerPo = customerPo;
    }

    @Override
    public CustomerRequestType getRequestType() {
        return CustomerRequestType.REPLICATE;
    }
}
