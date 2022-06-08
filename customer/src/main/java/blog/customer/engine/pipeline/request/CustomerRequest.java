package blog.customer.engine.pipeline.request;

public interface CustomerRequest  extends Request{
    CustomerRequestType getRequestType();
}
