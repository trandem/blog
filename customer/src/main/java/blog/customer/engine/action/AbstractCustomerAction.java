package blog.customer.engine.action;

import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.request.CustomerSharableRequest;
import blog.customer.engine.pipeline.response.CustomerResponse;

public abstract class AbstractCustomerAction implements CustomerAction {


    @Override
    public CustomerResponse doAction(CustomerRequest customerRequest) {
        if (customerRequest instanceof CustomerSharableRequest) {
            return null;
        } else {
            throw new RuntimeException("not implement yet");
        }
    }
}
