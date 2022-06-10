package blog.customer.engine.action.impl;

import blog.customer.engine.pipeline.request.impl.UpdateCustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponseResult;
import blog.customer.engine.pipeline.response.impl.UpdateCustomerResponse;
import blog.customer.storage.model.po.CustomerPo;

public class UpdateCustomerAction extends AbstractCustomerAction<UpdateCustomerRequest, UpdateCustomerResponse> {

    protected UpdateCustomerResponse doActionTransaction(UpdateCustomerRequest req) {
        UpdateCustomerResponse response = new UpdateCustomerResponse();
        CustomerPo prev = entityManager.getCustomer(req.getCustomerId());

        if (prev == null) throw new RuntimeException("invalid");

        CustomerPo next = req.getCustomerPo();

        if (!next.getVersion().equals(prev.getVersion())) throw new RuntimeException("invalid");
        next.setVersion(next.getVersion() + 1);

        response.setCustomerPo(next);
        response.setResult(CustomerResponseResult.SUCCESS);
        return response;
    }
}
