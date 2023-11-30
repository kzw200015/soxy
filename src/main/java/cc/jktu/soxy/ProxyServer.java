package cc.jktu.soxy;

import cc.jktu.soxy.config.SoxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProxyServer implements SmartLifecycle {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel serverChannel;
    private final SoxyConfig soxyConfig;

    public ProxyServer(SoxyConfig soxyConfig) {
        this.soxyConfig = soxyConfig;
    }

    @Override
    public void start() {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        public void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpServerHandler(soxyConfig));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = b.bind(soxyConfig.getPort()).sync().channel();
            log.info("ProxyServer started on port {}", soxyConfig.getPort());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        serverChannel.close();
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    @Override
    public boolean isRunning() {
        if (serverChannel == null) {
            return false;
        }

        return serverChannel.isActive();
    }

}
