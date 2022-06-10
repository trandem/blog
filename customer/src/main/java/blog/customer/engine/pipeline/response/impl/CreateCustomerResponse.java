package blog.customer.engine.pipeline.response.impl;

import blog.customer.engine.pipeline.response.CustomerResponse;
import blog.customer.engine.pipeline.response.CustomerResponseResult;
import blog.customer.storage.model.po.CustomerPo;

public class CreateCustomerResponse extends AbstractCustomerResponse {
    private CustomerPo po;

    public CreateCustomerResponse() {
    }

    public CreateCustomerResponse(CustomerResponseResult result, long id, long requestId, CustomerPo po) {
        super(result, id, requestId);
        this.po = po;
    }

    public CustomerPo getPo() {
        return po;
    }

    public void setPo(CustomerPo po) {
        this.po = po;
    }

    public static class CreateCustomerFactory implements CustomerResponse.CustomerFactory{

        @Override
        public CustomerResponse create() {
            return new CreateCustomerResponse();
        }
    }
}
