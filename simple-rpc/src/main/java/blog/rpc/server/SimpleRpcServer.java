package blog.rpc.server;

import blog.rpc.listener.RpcListener;
import blog.rpc.service.SimpleService;
import blog.rpc.service.impl.ServerImpl;
import blog.rpc.support.*;
import blog.rpc.tranport.imp.NettyTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SimpleRpcServer implements RpcListener<ByteBuf> {

    private Map<Integer, XMethod> methodService;

    private RPCSerialize serialize = RPCSerialize.serialize;

    public SimpleRpcServer(int port) {
        new NettyTransport(this, port);
        this.methodService = new HashMap<>();
    }

    public void addHandle(Object target) {
        Method[] methods = target.getClass().getDeclaredMethods();
        for (Method method : methods) {
            XMethod xMethod = new XMethod(method, target);
            methodService.put(xMethod.getSignature(), xMethod);
        }
    }

    public XFuture<ByteBuf> onMessage(ByteBuf buf) {
        byte[] bytes;
        int length = buf.readableBytes();
        if (buf.hasArray()) {
            bytes = buf.array();
        } else {
            bytes = new byte[length];
            buf.getBytes(buf.readerIndex(), bytes);
        }

        RPCRequest request = serialize.deSerialize(bytes, RPCRequest.class);
        try {
            Object result = this.methodService.get(request.getMethod()).invoke(request.getArgs());
            RPCResponse response = new RPCResponse(request.getId(), result);
            XFuture<ByteBuf> future = new XFuture<>();
            byte[]x = serialize.serialize(response);
            future.setResult(Unpooled.copiedBuffer(x));
            return future;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        SimpleRpcServer server = new SimpleRpcServer(9999);
        SimpleService service = new ServerImpl();
        server.addHandle(service);
    }
}
