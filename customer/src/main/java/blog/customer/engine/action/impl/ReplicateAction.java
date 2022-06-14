package blog.customer.engine.action.impl;

import blog.customer.engine.pipeline.request.impl.ReplicateCustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponseResult;
import blog.customer.engine.pipeline.response.impl.ReplicateCustomerResponse;
import blog.customer.storage.model.po.CustomerPo;

public class ReplicateAction extends AbstractCustomerAction<ReplicateCustomerRequest, ReplicateCustomerResponse> {


    @Override
    protected ReplicateCustomerResponse doActionTransaction(ReplicateCustomerRequest request) {
        ReplicateCustomerResponse response = new ReplicateCustomerResponse();

        CustomerPo next = request.getCustomerPo();
        assert next != null;
        CustomerPo prev = entityManager.getCustomer(next.getId());

        if (prev != null && next.getVersion() < prev.getVersion()) {
            throw new RuntimeException("stale version");
        }
        entityManager.putCustomer(next);

        response.setRequestId(request.getId());
        response.setResult(CustomerResponseResult.SUCCESS);

        return response;
    }
}
