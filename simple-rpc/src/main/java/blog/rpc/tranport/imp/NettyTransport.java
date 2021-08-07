package blog.rpc.tranport.imp;

import blog.rpc.listener.RpcListener;
import blog.rpc.tranport.RpcHandle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NettyTransport {
    private ServerBootstrap bootstrap;
    private EventLoopGroup group;
    private RpcListener listener;
    private int port;

    public NettyTransport(RpcListener listener, int port) {
        this.listener = listener;
        this.port = port;
        try {
            server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void server() throws Exception {
        this.group = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer() {
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new RpcHandle(listener));
                    }
                });
        ChannelFuture f = bootstrap.bind(port).sync(); // (5)
    }


}
