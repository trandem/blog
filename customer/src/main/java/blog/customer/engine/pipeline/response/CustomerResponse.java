package blog.customer.engine.pipeline.response;

public interface CustomerResponse extends Response {

    interface CustomerFactory {
        CustomerResponse create();
    }
}
