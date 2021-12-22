package blog.common.rpc.glosory;

import blog.serialize.base.*;

public class RpcRequest implements DMarshallable {

    private int methodSig;
    private int serviceId;
    private byte[] args;
    private String domain;
    private int busId;
    private long requestId;
    private String serviceName;

    public RpcRequest() {
    }

    public RpcRequest(int methodSig, int serviceId, byte[] args, String domain, int busId, long requestId, String serviceName) {
        this.methodSig = methodSig;
        this.serviceId = serviceId;
        this.args = args;
        this.domain = domain;
        this.busId = busId;
        this.requestId = requestId;
        this.serviceName = serviceName;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeInt(methodSig);
        output.writeInt(serviceId);
        output.writeBytes(args);
        output.writeString(domain);
        output.writeIntOptimise(busId);
        output.writeLongOptimise(requestId);
        output.writeString(serviceName);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.methodSig = input.readInt();
        this.serviceId = input.readInt();
        this.args = input.readBytes();
        this.domain = input.readString();
        this.busId = input.readIntPositiveOptimise();
        this.requestId = input.readLongPositiveOptimise();
        this.serviceName = input.readString();
    }

    public int getMethodSig() {
        return methodSig;
    }

    public void setMethodSig(int methodSig) {
        this.methodSig = methodSig;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public byte[] getArgs() {
        return args;
    }

    public void setArgs(byte[] args) {
        this.args = args;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public static class RpcRequestInstance implements DInstance<RpcRequest> {

        @Override
        public RpcRequest instance() {
            return new RpcRequest();
        }
    }
}
