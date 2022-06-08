package blog.customer.engine.signal;

import blog.customer.engine.pipeline.request.CustomerRequest;

public interface RequestSignal extends Signal {

    CustomerRequest getRequest();

}
