package blog.common.rpc.client;

import blog.common.concurrent.XFuture;
import blog.common.messenger.DMessenger;
import blog.common.messenger.DMessengerListener;
import blog.common.messenger.KafkaMessenger;
import blog.common.messenger.TransportTopic;
import blog.common.rpc.glosory.DService;
import blog.common.rpc.glosory.RpcRequest;
import blog.common.rpc.glosory.RpcResponse;
import blog.serialize.impl.DMarshallers;
import blog.serialize.base.DMarshaller;
import blog.serialize.impl.AllMarshaller;

import java.util.Map;
import java.util.concurrent.*;

public class DRpcClientImpl implements DRpcClient, DMessengerListener {

    private final Map<String, DService> registerService = new ConcurrentHashMap<>();
    public static final Map<Long, XFuture<RpcResponse>> futures = new ConcurrentHashMap<>();

    private DMessenger<Object> messenger = KafkaMessenger.getInstance();

    private TransportTopic rpcTopic;

    private final DMarshaller marshaller = AllMarshaller.DEFAULT;

    private ExecutorService transportExecutor;

    public DRpcClientImpl() {
        this.rpcTopic = messenger.registerTopic("rpc");
        this.transportExecutor = Executors.newFixedThreadPool(2);
    }

    public void start() {
        messenger.addListener(rpcTopic, this);
        messenger.setMarshaller(rpcTopic, marshaller);
        messenger.setExecutor(rpcTopic, transportExecutor);
    }

    @Override
    public void addService(DService service) {
        registerService.put(service.getDomain(), service);
    }

    @Override
    public DService getService(String name) {
        return registerService.get(name);
    }

    @Override
    public DService removeService(String name) {
        return registerService.remove(name);
    }

    @Override
    public Object sendRequest(RpcRequest request) throws InterruptedException, ExecutionException {
        XFuture<RpcResponse> future = new XFuture<>();
        futures.put(request.getRequestId(), future);
        messenger.send(request, rpcTopic);

        RpcResponse response = future.get();
        if (response.getErrorMsg().equals("")) {
            int methodSig = response.getMethodSig();
            if (methodSig != 0) {
                return DMarshallers.unMarshaller(marshaller, response.getResponse());
            } else {
                return null;
            }
        }
        throw new RuntimeException("server is wrong with error " + response.getErrorMsg());
    }

    @Override
    public Object sendRequest(String type, RpcRequest request) {
        throw new RuntimeException("do not implement");
    }

    @Override
    public Object sendRequest(Object... args) {
        return null;
    }

    @Override
    public Future<RpcResponse> sendAsyncRequest(RpcRequest request) {
        XFuture<RpcResponse> future = new XFuture<>();
        futures.put(request.getRequestId(), future);
        messenger.send(request, rpcTopic);
        return future;
    }

    @Override
    public void onMessage(Object data) {
        if (data instanceof RpcResponse) {
            RpcResponse response = (RpcResponse) data;
            if (response.getBusId() != Integer.parseInt(System.getProperty("APP_ID"))){
                XFuture<RpcResponse> rpcResponseXFuture = futures.get(response.getRequestId());
                rpcResponseXFuture.setResult(response);
            }
        }
    }
}
