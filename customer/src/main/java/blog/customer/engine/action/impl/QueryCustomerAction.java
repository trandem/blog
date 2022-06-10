package blog.customer.engine.action.impl;

import blog.customer.engine.pipeline.request.impl.QueryCustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponseResult;
import blog.customer.engine.pipeline.response.impl.QueryCustomerResponse;
import blog.customer.storage.model.po.CustomerPo;

public class QueryCustomerAction extends AbstractCustomerAction<QueryCustomerRequest, QueryCustomerResponse> {

    @Override
    protected QueryCustomerResponse doActionTransaction(QueryCustomerRequest queryCustomerRequest) {
        QueryCustomerResponse response = new QueryCustomerResponse();

        int customerId = queryCustomerRequest.getCustomerId();
        CustomerPo customerPo = entityManager.getCustomer(customerId);

        response.setCustomerPo(customerPo);
        if (customerPo != null) {
            response.setResult(CustomerResponseResult.SUCCESS);
        } else {
            response.setResult(CustomerResponseResult.INVALID);
        }

        return response;
    }
}
