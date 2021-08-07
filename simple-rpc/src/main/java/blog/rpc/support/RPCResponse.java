package blog.rpc.support;

public class RPCResponse {
    private long requestId;
    private Object result;

    public RPCResponse(long requestId, Object result) {
        this.requestId = requestId;
        this.result = result;
    }

    public RPCResponse() {
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
