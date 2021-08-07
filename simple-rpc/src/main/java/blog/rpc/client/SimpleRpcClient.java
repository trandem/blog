package blog.rpc.client;

import blog.rpc.listener.RpcListener;
import blog.rpc.service.SimpleService;
import blog.rpc.support.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SimpleRpcClient implements SimpleService, RpcListener<ByteBuf> {

    public static final Map<Long, XFuture> futures = new ConcurrentHashMap<>();
    private RPCSerialize serialize = RPCSerialize.serialize;

    private String host;
    private int port;

    private IdGenerator idGenerator = IdGenerator.instance;

    public SimpleRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
        connectNetty();
    }

    private volatile ClientHandle transport;

    public void connectNetty() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.option(ChannelOption.TCP_NODELAY,true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(transport = new ClientHandle(SimpleRpcClient.this));
                }
            });
            // Start the client.
            ChannelFuture f = b.connect(host, port).sync(); // (5)
            // Wait until the connection is closed.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int add(int a, int b) {
        int rs = Integer.MAX_VALUE;
        try {
            RPCRequest request = new RPCRequest();
            request.setId(idGenerator.nextId());
            Method method = this.getClass().getMethod("add", int.class, int.class);
            request.setMethod(XMethod.signature(method));
            request.setArgs(new Object[]{a, b});
            byte[] x = serialize.serialize(request);
            transport.sendMsg(Unpooled.copiedBuffer(x));
            XFuture<Integer> future = new XFuture<>();
            futures.put(request.getId(), future);
            rs = future.get(100, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    @Override
    public User getUser() {
        try {
            RPCRequest request = new RPCRequest();
            request.setId(idGenerator.nextId());
            Method method = this.getClass().getMethod("getUser");
            request.setMethod(XMethod.signature(method));
            request.setArgs(null);
            byte[] x = serialize.serialize(request);
            transport.sendMsg(Unpooled.copiedBuffer(x));
            XFuture<User> future = new XFuture<>();
            futures.put(request.getId(), future);
            return future.get(100, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public XFuture<ByteBuf> onMessage(ByteBuf buf) {
        byte[] bytes;
        int length = buf.readableBytes();
        if (buf.hasArray()) {
            bytes = buf.array();
        } else {
            bytes = new byte[length];
            buf.getBytes(buf.readerIndex(), bytes);
        }
        RPCResponse response = serialize.deSerialize(bytes, RPCResponse.class);
        if (futures.containsKey(response.getRequestId())) {
            futures.remove(response.getRequestId()).setResult(response.getResult());
        }
        return null;
    }


    public class ClientHandle extends SimpleChannelInboundHandler<ByteBuf> {
        private ChannelHandlerContext ctx;
        private RpcListener<ByteBuf> listener;

        public ClientHandle(RpcListener<ByteBuf> listener) {
            this.listener = listener;
        }

        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            this.ctx = ctx;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)  {
            if (listener != null) {
                listener.onMessage(msg);
            }
        }
        public void sendMsg(ByteBuf buf) {
            this.ctx.writeAndFlush(buf);
        }
    }

    public static void main(String[] args) {
        SimpleRpcClient client = new SimpleRpcClient("localhost", 9999);
        System.out.println(client.add(1, 3));
        System.out.println(client.getUser().getName());
    }
}
