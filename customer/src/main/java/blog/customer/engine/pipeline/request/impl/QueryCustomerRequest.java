package blog.customer.engine.pipeline.request.impl;

import blog.customer.engine.pipeline.request.CustomerRequestType;

public class QueryCustomerRequest extends AbstractCustomerSharableRequest {


    public QueryCustomerRequest(int customerId, long id) {
        super(customerId, id);
    }

    @Override
    public CustomerRequestType getRequestType() {
        return CustomerRequestType.GET_CUSTOMER;
    }

}
