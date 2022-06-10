package blog.customer.engine.pipeline.response.impl;

import blog.customer.engine.pipeline.response.CustomerResponse;
import blog.customer.engine.pipeline.response.CustomerResponseResult;

public class AbstractCustomerResponse implements CustomerResponse {

    private CustomerResponseResult result;
    private long id;
    private long requestId;

    public AbstractCustomerResponse() {
    }

    public AbstractCustomerResponse(CustomerResponseResult result, long id, long requestId) {
        this.result = result;
        this.id = id;
        this.requestId = requestId;
    }

    @Override
    public long getRequestId() {
        return this.requestId;
    }

    @Override
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Result getResult() {
        return result;
    }

    @Override
    public void setResult(Result result) {
        this.result = (CustomerResponseResult) result;
    }

}
