package blog.customer.engine.pipeline.response.impl;

import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.request.CustomerRequestType;
import blog.customer.engine.pipeline.response.CustomerFactoryManager;
import blog.customer.engine.pipeline.response.CustomerResponse;

import java.util.HashMap;
import java.util.Map;

public class CustomerFactoryManagerImpl implements CustomerFactoryManager {
    private Map<CustomerRequestType, CustomerResponse.CustomerFactory> map = new HashMap<>();

    public CustomerFactoryManagerImpl() {

    }

    private void initFactory() {

        map.put(CustomerRequestType.GET_CUSTOMER, new QueryCustomerResponse.QueryCustomerResponseFactory());
        map.put(CustomerRequestType.CREATE, new CreateCustomerResponse.CreateCustomerFactory());
        map.put(CustomerRequestType.UPDATE, new UpdateCustomerResponse.UpdateCustomerResponseFactory());
        map.put(CustomerRequestType.REPLICATE, new ReplicateCustomerResponse.ReplicateCustomerResponseFactory());

    }

    @Override
    public CustomerResponse createCustomerResponse(CustomerRequest request) {
        CustomerResponse.CustomerFactory factory = map.get(request.getRequestType());
        if (factory == null) {
            throw new RuntimeException("not implement factory");
        }

        return factory.create();
    }

}