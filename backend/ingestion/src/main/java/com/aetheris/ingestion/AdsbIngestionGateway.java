package com.aetheris.ingestion;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class AdsbIngestionGateway {
    private static final Logger logger = LoggerFactory.getLogger(AdsbIngestionGateway.class);
    private final int port = 30001; // Standard SBS-1 port for ADS-B
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void start() {
        Thread.ofVirtual().start(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                                ch.pipeline().addLast(new StringDecoder());
                                ch.pipeline().addLast(new AdsbHandler());
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                logger.info("ADS-B Ingestion Gateway starting on port {}", port);
                ChannelFuture f = b.bind(port).sync();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                logger.error("ADS-B Gateway interrupted", e);
                Thread.currentThread().interrupt();
            } finally {
                stop();
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    private static class AdsbHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            // In a real scenario, parse SBS-1 or raw BEAST format here
            // For now, log the raw telemetry hit
            logger.debug("Received ADS-B Telemetry: {}", msg);
        }
    }
}
