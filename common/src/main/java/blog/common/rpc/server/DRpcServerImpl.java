package blog.common.rpc.server;

import blog.common.cluster.glosory.ServiceBus;
import blog.common.messenger.DMessenger;
import blog.common.messenger.DMessengerListener;
import blog.common.messenger.KafkaMessenger;
import blog.common.messenger.TransportTopic;
import blog.common.rpc.glosory.DService;
import blog.common.rpc.glosory.RpcRequest;
import blog.common.rpc.glosory.RpcResponse;
import blog.common.rpc.service.DServiceIml;
import blog.common.rpc.service.SimpleImpl;
import blog.serialize.impl.DMarshallers;
import blog.serialize.base.DMarshaller;
import blog.serialize.impl.AllMarshaller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DRpcServerImpl implements DMessengerListener, DRpcServer {

    private final Map<String, DService> registerService = new ConcurrentHashMap<>();

    private ServiceBus bus;

    private final DMarshaller marshaller = AllMarshaller.DEFAULT;

    private ExecutorService[] shardingExecutor;

    private ExecutorService transportExecutor;

    private DMessenger<Object> messenger = KafkaMessenger.getInstance();

    private TransportTopic rpcTopic;


    private DServiceIml.ServiceFactory factory = new DServiceIml.ServiceFactory();

    public DRpcServerImpl() {
        this.shardingExecutor = new ExecutorService[5];
        for (int i =0 ;i < shardingExecutor.length ; i++){
            shardingExecutor[i] = Executors.newSingleThreadExecutor();
        }
        this.transportExecutor = Executors.newFixedThreadPool(2);
        this.rpcTopic = messenger.registerTopic("rpc");
    }

    public void start(){
        messenger.addListener(rpcTopic, this);
        messenger.setMarshaller(rpcTopic, marshaller);
        messenger.setExecutor(rpcTopic, transportExecutor);
    }

    @Override
    public void onMessage(Object data) {
        if (data instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) data;
            if (request.getBusId() != Integer.parseInt(System.getProperty("APP_ID"))) {
                final int serviceId = Math.abs(request.getServiceName().hashCode());
                shardingExecutor[serviceId % shardingExecutor.length].submit(() -> {
                    RpcResponse response = new RpcResponse();
                    response.setRequestId(request.getRequestId());
                    response.setMethodSig(request.getMethodSig());

                    try {
                        DService service = registerService.get(request.getServiceName());
                        int methodSig = request.getMethodSig();
                        if (methodSig != 0) {
                            Object[] args = DMarshallers.unMarshaller(marshaller, request.getArgs());
                            Object result = service.invoke(request.getMethodSig(), args);
                            byte[] payload = DMarshallers.marshaller(result, marshaller);
                            response.setResponse(payload);
                        }
                    } catch (Throwable throwable) {
                        String errorMsg = throwable.getMessage() == null ? "null": throwable.getMessage() ;
                        response.setErrorMsg(errorMsg);
                        throwable.printStackTrace();
                    } finally {
                        //response to client
                        messenger.send(response, rpcTopic);
                    }
                });
            }
        }
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

    public Map<String, DService> getRegisterService() {
        return registerService;
    }

    public ServiceBus getBus() {
        return bus;
    }

    public void setBus(ServiceBus bus) {
        this.bus = bus;
    }

    public DMessenger<Object> getMessenger() {
        return messenger;
    }

    public void setMessenger(DMessenger<Object> messenger) {
        this.messenger = messenger;
    }

    @Override
    public void createService(String name,Object object){
        DService service =factory
                .create(Integer.parseInt(System.getProperty("APP_ID")),name,object);
        addService(service);
    }
    public static void main(String[] args) {

        DRpcServerImpl rpcServer  = new DRpcServerImpl();
        rpcServer.createService("simple",new SimpleImpl());
        rpcServer.start();
    }
}
