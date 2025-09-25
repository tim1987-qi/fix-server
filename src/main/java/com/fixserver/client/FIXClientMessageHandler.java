package com.fixserver.client;

import com.fixserver.core.FIXMessage;

/**
 * Handler interface for processing incoming FIX messages from the server.
 * 
 * Implementations of this interface can be registered with a FIX client
 * to receive and process messages sent by the server.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@FunctionalInterface
public interface FIXClientMessageHandler {
    
    /**
     * Called when a FIX message is received from the server.
     * 
     * This method is called on a background thread, so implementations
     * should be thread-safe and avoid blocking operations.
     * 
     * @param message the received FIX message
     * @param client the client that received the message
     */
    void onMessage(FIXMessage message, FIXClient client);
}