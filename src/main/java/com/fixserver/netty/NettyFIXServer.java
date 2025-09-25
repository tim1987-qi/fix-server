package com.fixserver.netty;

import com.fixserver.protocol.FIXProtocolHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance Netty-based FIX Protocol Server.
 * 
 * This server provides better scalability and performance compared to 
 * traditional socket-based implementations by using non-blocking I/O
 * and event-driven architecture.
 * 
 * Features:
 * - Non-blocking I/O with Netty NIO
 * - Event-driven message processing
 * - Connection pooling and management
 * - Built-in backpressure handling
 * - Configurable thread pools
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class NettyFIXServer {
    
    @Value("${fix.server.netty.port:9879}")
    private int nettyPort;
    
    @Value("${fix.server.netty.boss-threads:1}")
    private int bossThreads;
    
    @Value("${fix.server.netty.worker-threads:0}") // 0 = use default (2 * CPU cores)
    private int workerThreads;
    
    @Value("${fix.server.netty.enabled:true}")
    private boolean nettyEnabled;
    
    @Autowired
    private FIXProtocolHandler protocolHandler;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * Starts the Netty FIX server when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        if (!nettyEnabled) {
            log.info("Netty FIX Server is disabled");
            return;
        }
        
        if (running.get()) {
            log.warn("Netty FIX Server is already running");
            return;
        }
        
        try {
            // Create event loop groups
            bossGroup = new NioEventLoopGroup(bossThreads);
            workerGroup = new NioEventLoopGroup(workerThreads);
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // Add FIX message decoder/encoder
                            pipeline.addLast("fixDecoder", new FIXMessageDecoder());
                            pipeline.addLast("fixEncoder", new FIXMessageEncoder());
                            
                            // Add FIX message handler
                            pipeline.addLast("fixHandler", new FIXMessageHandler(protocolHandler));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 32 * 1024);
            
            // Bind and start to accept incoming connections
            ChannelFuture future = bootstrap.bind(nettyPort).sync();
            serverChannel = future.channel();
            running.set(true);
            
            log.info("Netty FIX Server started on port {} with {} boss threads and {} worker threads", 
                    nettyPort, bossThreads, workerThreads == 0 ? "default" : workerThreads);
            log.info("Server channel: {}", serverChannel);
            
        } catch (Exception e) {
            log.error("Failed to start Netty FIX Server on port {}", nettyPort, e);
            stopServer();
        }
    }
    
    /**
     * Stops the Netty FIX server and releases all resources.
     */
    @PreDestroy
    public void stopServer() {
        if (!running.get()) {
            return;
        }
        
        log.info("Stopping Netty FIX Server...");
        running.set(false);
        
        try {
            // Close server channel
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (Exception e) {
            log.error("Error closing server channel", e);
        }
        
        // Shutdown event loop groups
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        log.info("Netty FIX Server stopped");
    }
    
    /**
     * Returns whether the server is currently running.
     */
    public boolean isRunning() {
        return running.get() && serverChannel != null && serverChannel.isActive();
    }
    
    /**
     * Returns the port the server is listening on.
     */
    public int getPort() {
        return nettyPort;
    }
    
    /**
     * Returns the number of active channels (connections).
     */
    public int getActiveConnections() {
        // This would be implemented with a connection manager
        // For now, return 0 as placeholder
        return 0;
    }
}