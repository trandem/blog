package blog.customer.engine.pipeline.response;

public interface Response {
    long getId();

    long setId(long id);

    Result getResult();

    void setResult(Result result);

    interface Result {
        boolean isSuccess();
    }
}
