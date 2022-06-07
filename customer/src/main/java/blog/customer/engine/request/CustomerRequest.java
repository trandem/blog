package blog.customer.engine.request;

public interface CustomerRequest {

    long getId();

    long setId(long id);

    CustomerRequestType getRequestType();
}
