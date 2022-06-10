package blog.customer.engine.action.impl;

import blog.customer.engine.pipeline.request.impl.CreateCustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponseResult;
import blog.customer.engine.pipeline.response.impl.CreateCustomerResponse;
import blog.customer.storage.model.glosory.CustomerStatus;
import blog.customer.storage.model.po.CustomerPo;

public class CreateAccountAction extends AbstractCustomerAction<CreateCustomerRequest, CreateCustomerResponse> {

    @Override
    protected CreateCustomerResponse doActionTransaction(CreateCustomerRequest queryCustomerRequest) {
        CreateCustomerResponse response = new CreateCustomerResponse();
        CustomerPo po = response.getPo();
        po.setVersion(1);
        po.setStatus(CustomerStatus.ENABLE);

        response.setPo(po);
        response.setResult(CustomerResponseResult.SUCCESS);

        return response;
    }
}
