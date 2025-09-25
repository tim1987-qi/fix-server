package com.fixserver.client;

import com.fixserver.core.FIXMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for FIX client implementations.
 * 
 * Provides the contract for connecting to FIX servers, sending messages,
 * and handling responses. Supports both synchronous and asynchronous operations.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public interface FIXClient {
    
    /**
     * Connects to the FIX server and initiates the logon sequence.
     * 
     * @return CompletableFuture that completes when connection is established
     * @throws FIXClientException if connection fails
     */
    CompletableFuture<Void> connect();
    
    /**
     * Disconnects from the FIX server gracefully.
     * 
     * @return CompletableFuture that completes when disconnection is complete
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Sends a FIX message to the server.
     * 
     * @param message the FIX message to send
     * @return CompletableFuture that completes when message is sent
     * @throws FIXClientException if message cannot be sent
     */
    CompletableFuture<Void> sendMessage(FIXMessage message);
    
    /**
     * Sends a FIX message and waits for a response.
     * 
     * @param message the FIX message to send
     * @return CompletableFuture containing the response message
     * @throws FIXClientException if message cannot be sent or no response received
     */
    CompletableFuture<FIXMessage> sendAndWaitForResponse(FIXMessage message);
    
    /**
     * Checks if the client is currently connected to the server.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Gets the current session ID.
     * 
     * @return the session ID, or null if not connected
     */
    String getSessionId();
    
    /**
     * Registers a message handler for incoming messages.
     * 
     * @param handler the message handler
     */
    void setMessageHandler(FIXClientMessageHandler handler);
    
    /**
     * Registers a connection state handler.
     * 
     * @param handler the connection state handler
     */
    void setConnectionHandler(FIXClientConnectionHandler handler);
}