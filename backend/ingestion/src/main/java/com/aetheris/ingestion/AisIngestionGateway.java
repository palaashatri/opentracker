package com.aetheris.ingestion;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class AisIngestionGateway {
    private static final Logger logger = LoggerFactory.getLogger(AisIngestionGateway.class);
    private final int port = 10110; // Standard port for AIS over UDP
    private EventLoopGroup group;

    @PostConstruct
    public void start() {
        Thread.ofVirtual().start(() -> {
            group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                                String msg = packet.content().toString(CharsetUtil.UTF_8);
                                logger.debug("Received AIS Message: {}", msg);
                                // In production, NMEA AIVDM/AIVDO parsing happens here
                            }
                        });

                logger.info("AIS (Maritime) Ingestion Gateway starting on UDP port {}", port);
                b.bind(port).sync().channel().closeFuture().sync();
            } catch (InterruptedException e) {
                logger.error("AIS Gateway interrupted", e);
                Thread.currentThread().interrupt();
            } finally {
                stop();
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (group != null) group.shutdownGracefully();
    }
}
