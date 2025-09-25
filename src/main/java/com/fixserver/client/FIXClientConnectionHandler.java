package com.fixserver.client;

/**
 * Handler interface for FIX client connection state changes.
 * 
 * Implementations of this interface can be registered with a FIX client
 * to receive notifications about connection state changes.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public interface FIXClientConnectionHandler {
    
    /**
     * Called when the client successfully connects to the server.
     * 
     * @param client the client that connected
     */
    void onConnected(FIXClient client);
    
    /**
     * Called when the client disconnects from the server.
     * 
     * @param client the client that disconnected
     * @param reason the reason for disconnection
     */
    void onDisconnected(FIXClient client, String reason);
    
    /**
     * Called when a connection error occurs.
     * 
     * @param client the client that experienced the error
     * @param error the error that occurred
     */
    void onError(FIXClient client, Throwable error);
    
    /**
     * Called when the logon process completes successfully.
     * 
     * @param client the client that logged on
     */
    void onLoggedOn(FIXClient client);
    
    /**
     * Called when the client logs out from the server.
     * 
     * @param client the client that logged out
     */
    void onLoggedOut(FIXClient client);
}