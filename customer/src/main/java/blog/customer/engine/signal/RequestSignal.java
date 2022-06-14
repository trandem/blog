package blog.customer.engine.signal;

import blog.common.concurrent.DFuture;
import blog.customer.engine.pipeline.request.CustomerRequest;
import blog.customer.engine.pipeline.response.CustomerResponse;

public interface RequestSignal extends Signal {

    CustomerRequest getRequest();

     DFuture<CustomerResponse> getCustomerFuture();

     void setCustomerFuture(DFuture<CustomerResponse> customerFuture) ;

}
