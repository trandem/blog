package blog.customer.engine.action;

import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponse;

public interface CustomerAction extends Action<CustomerRequest, CustomerResponse> {
}
