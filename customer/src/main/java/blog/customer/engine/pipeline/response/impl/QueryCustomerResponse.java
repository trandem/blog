package blog.customer.engine.pipeline.response.impl;

import blog.customer.engine.pipeline.response.CustomerResponse;
import blog.customer.engine.pipeline.response.CustomerResponseResult;
import blog.customer.storage.model.po.CustomerPo;

public class QueryCustomerResponse extends AbstractCustomerResponse {

    private CustomerPo customerPo;

    public QueryCustomerResponse(CustomerResponseResult result, long id, long requestId, CustomerPo customerPo) {
        super(result, id, requestId);
        this.customerPo = customerPo;
    }

    public QueryCustomerResponse() {
    }

    public CustomerPo getCustomerPo() {
        return customerPo;
    }

    public void setCustomerPo(CustomerPo customerPo) {
        this.customerPo = customerPo;
    }

    public static class QueryCustomerResponseFactory implements CustomerResponse.CustomerFactory{

        @Override
        public CustomerResponse create() {
            return new QueryCustomerResponse();
        }
    }
}
