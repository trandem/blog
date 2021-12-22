package blog.common.rpc.client;

import blog.common.rpc.glosory.DService;
import blog.common.rpc.glosory.RpcRequest;
import blog.common.rpc.glosory.RpcResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface DRpcClient {

    void addService(DService service);

    DService getService(String  name);

    DService removeService(String  name);

    Object sendRequest(RpcRequest request) throws InterruptedException, ExecutionException;

    Object sendRequest(String type, RpcRequest request);

    Object sendRequest(Object... args);

    Future<RpcResponse> sendAsyncRequest(RpcRequest request);
}
