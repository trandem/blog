package blog.customer.engine.pipeline.response;

import blog.customer.engine.pipeline.request.CustomerRequest;

public interface CustomerFactoryManager {


    CustomerResponse createCustomerResponse(CustomerRequest request);
}
