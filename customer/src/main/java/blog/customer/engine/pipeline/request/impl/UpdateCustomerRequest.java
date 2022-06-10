package blog.customer.engine.pipeline.request.impl;

import blog.customer.engine.pipeline.request.CustomerRequestType;
import blog.customer.storage.model.po.CustomerPo;


public class UpdateCustomerRequest extends AbstractCustomerSharableRequest {

    private CustomerPo customerPo;

    public UpdateCustomerRequest(int customerId, long id, CustomerPo customerPo) {
        super(customerId, id);
        this.customerPo = customerPo;
    }

    public CustomerPo getCustomerPo() {
        return customerPo;
    }


    @Override
    public CustomerRequestType getRequestType() {
        return CustomerRequestType.UPDATE;
    }

}
