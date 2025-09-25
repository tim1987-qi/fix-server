package com.fixserver.client;

/**
 * Factory class for creating FIX client instances.
 * 
 * Provides convenient methods for creating FIX clients with different
 * configurations and connection types.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public class FIXClientFactory {
    
    /**
     * Creates a new FIX client with the specified configuration.
     * 
     * @param config the client configuration
     * @return a new FIX client instance
     */
    public static FIXClient createClient(FIXClientConfiguration config) {
        return new FIXClientImpl(config);
    }
    
    /**
     * Creates a new FIX client with basic configuration.
     * 
     * @param host the server host
     * @param port the server port
     * @param senderCompId the sender company ID
     * @param targetCompId the target company ID
     * @return a new FIX client instance
     */
    public static FIXClient createClient(String host, int port, String senderCompId, String targetCompId) {
        FIXClientConfiguration config = FIXClientConfiguration.defaultConfig(host, port, senderCompId, targetCompId);
        return new FIXClientImpl(config);
    }
    
    /**
     * Creates a builder for constructing FIX client configurations.
     * 
     * @return a new configuration builder
     */
    public static FIXClientConfiguration.FIXClientConfigurationBuilder builder() {
        return FIXClientConfiguration.builder();
    }
}