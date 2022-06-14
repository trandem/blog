package blog.customer.engine.pipeline.response.impl;

import blog.customer.engine.pipeline.response.CustomerResponse;

public class ReplicateCustomerResponse extends AbstractCustomerResponse {

    @Override
    public String toString() {
        return "ReplicateCustomerResponse{" +
                "result=" + result +
                ", id=" + id +
                ", requestId=" + requestId +
                '}';
    }

    public static class ReplicateCustomerResponseFactory implements CustomerResponse.CustomerFactory {

        @Override
        public CustomerResponse create() {
            return new ReplicateCustomerResponse();
        }

    }
}
