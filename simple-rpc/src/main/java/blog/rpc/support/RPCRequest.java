package blog.rpc.support;

public class RPCRequest {
    private int method;
    private Object[] args;
    private long id;

    public RPCRequest(int method, Object[] args, long id) {
        this.method = method;
        this.args = args;
        this.id = id;
    }

    public RPCRequest() {
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
