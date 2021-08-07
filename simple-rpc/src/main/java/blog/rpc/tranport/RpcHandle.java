package blog.rpc.tranport;

import blog.rpc.listener.RpcListener;

import blog.rpc.support.XFuture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class RpcHandle extends SimpleChannelInboundHandler<ByteBuf> {

    private RpcListener<ByteBuf> listener;

    public RpcHandle(RpcListener<ByteBuf> listener) {
        this.listener = listener;
    }

    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if (listener != null) {
            XFuture<ByteBuf> rs = listener.onMessage(byteBuf);
            ctx.writeAndFlush(rs.get());
        }
    }
}
