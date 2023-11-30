package cc.jktu.soxy;

import cc.jktu.soxy.config.Protocol;
import cc.jktu.soxy.config.ProxyConfig;
import cc.jktu.soxy.config.SoxyConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;

@Slf4j
public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    private final SoxyConfig soxyConfig;
    private Channel socksChannel;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("[{}] http channel inactive", ctx.channel().id());
        if (socksChannel != null) {
            socksChannel.close();
        }
    }

    public HttpServerHandler(SoxyConfig soxyConfig) {
        this.soxyConfig = soxyConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpRequest request) {
            final String host = request.headers().get("Host");
            final ProxyConfig proxyConfig = soxyConfig.getProxyConfigs().get(host.split(":")[0]);
            if (proxyConfig == null) {
                ctx.close();
                return;
            }
            final URI upstreamUri = URI.create(proxyConfig.getUpstream());
            final URI socksUri = URI.create(proxyConfig.getSocks());
            request.headers().set("Host", upstreamUri.getHost());
            log.debug("[{}] from [{}] proxy to [{}] using [{}]", ctx.channel().id(), host, upstreamUri + request.uri(), socksUri);
            if (socksChannel == null || !socksChannel.isActive()) {
                socksChannel = getChannel(upstreamUri, socksUri, ctx);
            }
            socksChannel.writeAndFlush(msg);
        } else if (msg instanceof LastHttpContent) {
            socksChannel.writeAndFlush(msg);
            log.debug("[{}] proxy completed", ctx.channel().id());
        } else if (msg instanceof HttpContent) {
            socksChannel.writeAndFlush(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage());
        ctx.close();
    }

    @SneakyThrows
    private Channel getChannel(URI upstreamUri, URI socksUri, ChannelHandlerContext ctx) {
        final Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .resolver(NoopAddressResolverGroup.INSTANCE)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    public void initChannel(NioSocketChannel ch) throws Exception {
                        final Socks5ProxyHandler socks5ProxyHandler = getSocks5ProxyHandler(socksUri);
                        ch.pipeline().addLast(socks5ProxyHandler);
                        if (upstreamUri.getScheme().equals(Protocol.HTTPS.name().toLowerCase())) {
                            final SslHandler sslHandler = SslContextBuilder.forClient().build().newHandler(ch.alloc(), upstreamUri.getHost(), upstreamUri.getPort());
                            ch.pipeline().addLast(sslHandler);
                        }
                        ch.pipeline()
                                .addLast(new HttpRequestEncoder())
                                .addLast(new HttpRespWriteBackHandler(ctx.channel()));
                    }
                });
        return bootstrap.connect(upstreamUri.getHost(), upstreamUri.getPort()).channel();
    }

    private static Socks5ProxyHandler getSocks5ProxyHandler(URI socksUri) {
        final Socks5ProxyHandler socks5ProxyHandler;

        final String userInfo = socksUri.getUserInfo();
        if (userInfo == null) {
            socks5ProxyHandler = new Socks5ProxyHandler(new InetSocketAddress(socksUri.getHost(), socksUri.getPort()));
        } else {
            final String[] auth = userInfo.split(":");
            socks5ProxyHandler = new Socks5ProxyHandler(new InetSocketAddress(socksUri.getHost(), socksUri.getPort()), auth[0], auth[1]);
        }
        return socks5ProxyHandler;
    }

}