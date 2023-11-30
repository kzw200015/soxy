package cc.jktu.soxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpRespWriteBackHandler extends ChannelInboundHandlerAdapter {

    private final Channel parentChannel;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("[{}] socks channel inactive", ctx.channel().id());
    }

    public HttpRespWriteBackHandler(Channel parentChannel) {
        this.parentChannel = parentChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        parentChannel.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage());
        ctx.close();
    }

}
