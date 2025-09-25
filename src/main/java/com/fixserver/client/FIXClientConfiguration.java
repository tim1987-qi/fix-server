package com.fixserver.client;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Configuration class for FIX client connections.
 * 
 * Contains all the necessary configuration parameters for establishing
 * and maintaining a FIX connection to a server.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
public class FIXClientConfiguration {
    
    /**
     * The hostname or IP address of the FIX server.
     */
    private final String host;
    
    /**
     * The port number of the FIX server.
     */
    private final int port;
    
    /**
     * The sender company ID (tag 49).
     */
    private final String senderCompId;
    
    /**
     * The target company ID (tag 56).
     */
    private final String targetCompId;
    
    /**
     * The FIX version to use (e.g., "FIX.4.4").
     */
    @Builder.Default
    private final String fixVersion = "FIX.4.4";
    
    /**
     * The heartbeat interval in seconds.
     */
    @Builder.Default
    private final int heartbeatInterval = 30;
    
    /**
     * Connection timeout duration.
     */
    @Builder.Default
    private final Duration connectionTimeout = Duration.ofSeconds(30);
    
    /**
     * Logon timeout duration.
     */
    @Builder.Default
    private final Duration logonTimeout = Duration.ofSeconds(30);
    
    /**
     * Whether to reset sequence numbers on logon.
     */
    @Builder.Default
    private final boolean resetSeqNumFlag = false;
    
    /**
     * Whether to enable message validation.
     */
    @Builder.Default
    private final boolean validateMessages = true;
    
    /**
     * Whether to enable automatic heartbeat responses.
     */
    @Builder.Default
    private final boolean autoHeartbeat = true;
    
    /**
     * Whether to enable automatic resend requests for sequence gaps.
     */
    @Builder.Default
    private final boolean autoResendRequest = true;
    
    /**
     * Maximum number of reconnection attempts.
     */
    @Builder.Default
    private final int maxReconnectAttempts = 3;
    
    /**
     * Delay between reconnection attempts.
     */
    @Builder.Default
    private final Duration reconnectDelay = Duration.ofSeconds(5);
    
    /**
     * Whether to enable TLS/SSL encryption.
     */
    @Builder.Default
    private final boolean tlsEnabled = false;
    
    /**
     * Path to the truststore file for TLS connections.
     */
    private final String truststorePath;
    
    /**
     * Password for the truststore.
     */
    private final String truststorePassword;
    
    /**
     * Path to the keystore file for client certificate authentication.
     */
    private final String keystorePath;
    
    /**
     * Password for the keystore.
     */
    private final String keystorePassword;
    
    /**
     * Username for authentication (if required by server).
     */
    private final String username;
    
    /**
     * Password for authentication (if required by server).
     */
    private final String password;
    
    /**
     * Creates a default configuration for testing.
     * 
     * @param host the server host
     * @param port the server port
     * @param senderCompId the sender company ID
     * @param targetCompId the target company ID
     * @return a default configuration
     */
    public static FIXClientConfiguration defaultConfig(String host, int port, 
                                                      String senderCompId, String targetCompId) {
        return FIXClientConfiguration.builder()
                .host(host)
                .port(port)
                .senderCompId(senderCompId)
                .targetCompId(targetCompId)
                .build();
    }
}