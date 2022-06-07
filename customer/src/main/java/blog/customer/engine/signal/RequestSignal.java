package blog.customer.engine.signal;

import blog.customer.engine.request.CustomerRequest;

public interface RequestSignal {

    int shard();

    void setShard(int shard);

    CustomerRequest getRequest();

}
