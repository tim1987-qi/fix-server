package com.fixserver.netty;

import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.performance.OptimizedNettyDecoder;
import com.fixserver.performance.HighPerformanceMessageParser;
import com.fixserver.performance.JVMOptimizationConfig;
import com.fixserver.config.NettyConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
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

    @Autowired
    private HighPerformanceMessageParser highPerformanceParser;

    @Autowired
    private JVMOptimizationConfig jvmOptimizationConfig;

    @Autowired
    private NettyConfiguration nettyConfig;

    @Value("${fix.server.performance.enabled:true}")
    private boolean performanceOptimizationsEnabled;

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

                            // Add FIX message decoder/encoder - use optimized version if enabled
                            if (performanceOptimizationsEnabled) {
                                pipeline.addLast("fixDecoder", new OptimizedNettyDecoder());
                                log.debug("Using OptimizedNettyDecoder for high performance");
                            } else {
                                pipeline.addLast("fixDecoder", new FIXMessageDecoder());
                                log.debug("Using standard FIXMessageDecoder");
                            }
                            pipeline.addLast("fixEncoder", new FIXMessageEncoder());

                            // Add FIX message handler
                            pipeline.addLast("fixHandler", new FIXMessageHandler(protocolHandler));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, nettyConfig.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, nettyConfig.isReuseAddress())
                    .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.isKeepAlive())
                    .childOption(ChannelOption.TCP_NODELAY, nettyConfig.isTcpNoDelay())
                    .childOption(ChannelOption.SO_RCVBUF, nettyConfig.getReceiveBufferSize())
                    .childOption(ChannelOption.SO_SNDBUF, nettyConfig.getSendBufferSize())
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyConfig.getConnectTimeoutMillis())
                    .childOption(ChannelOption.WRITE_SPIN_COUNT, nettyConfig.getWriteSpinCount());

            // Apply performance optimizations
            if (performanceOptimizationsEnabled) {
                if (nettyConfig.isPooledAllocator()) {
                    bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
                    bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
                    log.debug("Using PooledByteBufAllocator for better memory management");
                } else {
                    bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
                    bootstrap.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
                }
                
                // Additional performance options
                bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, 
                    new WriteBufferWaterMark(32 * 1024, 64 * 1024));
                log.debug("Applied performance optimizations to Netty bootstrap");
            }

            // Bind and start to accept incoming connections
            ChannelFuture future = bootstrap.bind(nettyPort).sync();
            serverChannel = future.channel();
            running.set(true);

            log.info("Netty FIX Server started on port {} with {} boss threads and {} worker threads",
                    nettyPort, bossThreads, workerThreads == 0 ? "default" : workerThreads);
            log.info("Performance optimizations enabled: {}", performanceOptimizationsEnabled);
            log.info("Server channel: {}", serverChannel);
            
            // JVM optimizations are applied automatically via @EventListener
            if (performanceOptimizationsEnabled && jvmOptimizationConfig != null) {
                log.info("JVM performance optimizations are active");
            }

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