package blog.common.rpc.glosory;

import blog.serialize.base.*;

public class RpcResponse implements DMarshallable {
    private long requestId;
    private long id;
    private int methodSig;
    private String errorMsg;
    private byte [] response;
    private int busId;

    public RpcResponse() {
        errorMsg ="";
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeLongOptimise(requestId);
        output.writeLongOptimise(id);
        output.writeInt(methodSig);
        output.writeString(errorMsg);
        output.writeBytes(response);
        output.writeInt(busId);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.requestId = input.readLongPositiveOptimise();
        this.id = input.readLongPositiveOptimise();
        this.methodSig = input.readInt();
        this.errorMsg = input.readString();
        this.response = input.readBytes();
        this.busId = input.readInt();
    }

    public static class RpcResponseInstance implements DInstance<RpcResponse> {

        @Override
        public RpcResponse instance() {
            return new RpcResponse();
        }
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getMethodSig() {
        return methodSig;
    }

    public void setMethodSig(int methodSig) {
        this.methodSig = methodSig;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public byte[] getResponse() {
        return response;
    }

    public void setResponse(byte[] response) {
        this.response = response;
    }

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }
}
