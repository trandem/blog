package blog.customer.engine.pipeline.response;

public interface Response {

    long getRequestId();

    void setRequestId(long requestId);

    long getId();

    void setId(long id);

    Result getResult();

    void setResult(Result result);

    interface Result {
        boolean isSuccess();
    }
}
