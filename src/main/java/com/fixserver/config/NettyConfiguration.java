package com.fixserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Netty FIX Server.
 * 
 * This configuration class allows customization of Netty server settings
 * through application properties.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fix.server.netty")
public class NettyConfiguration {
    
    /**
     * Whether Netty FIX server is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Port for Netty FIX server.
     */
    private int port = 9879;
    
    /**
     * Number of boss threads (acceptor threads).
     * Default is 1, which is usually sufficient.
     */
    private int bossThreads = 1;
    
    /**
     * Number of worker threads (I/O threads).
     * Default is 0, which means use Netty's default (2 * CPU cores).
     */
    private int workerThreads = 0;
    
    /**
     * SO_BACKLOG option for server socket.
     */
    private int backlog = 128;
    
    /**
     * SO_RCVBUF option for client sockets.
     */
    private int receiveBufferSize = 32 * 1024; // 32KB
    
    /**
     * SO_SNDBUF option for client sockets.
     */
    private int sendBufferSize = 32 * 1024; // 32KB
    
    /**
     * SO_KEEPALIVE option for client sockets.
     */
    private boolean keepAlive = true;
    
    /**
     * TCP_NODELAY option for client sockets.
     */
    private boolean tcpNoDelay = true;
    
    /**
     * SO_REUSEADDR option for server socket.
     */
    private boolean reuseAddress = true;
    
    /**
     * Session timeout in seconds.
     */
    private int sessionTimeoutSeconds = 300; // 5 minutes
    
    /**
     * Heartbeat interval in seconds.
     */
    private int heartbeatIntervalSeconds = 30;
    
    /**
     * Maximum number of concurrent sessions.
     */
    private int maxSessions = 1000;
}