package blog.rpc.tranport;

public interface RPCTransport {
    void read(byte[] arr);

    void write();
}
