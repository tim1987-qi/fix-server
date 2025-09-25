package com.fixserver.client;

/**
 * Exception thrown by FIX client operations.
 * 
 * This exception is used to indicate various error conditions that can occur
 * during FIX client operations such as connection failures, message sending
 * errors, or protocol violations.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public class FIXClientException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new FIX client exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public FIXClientException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new FIX client exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public FIXClientException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new FIX client exception with the specified cause.
     * 
     * @param cause the cause of the exception
     */
    public FIXClientException(Throwable cause) {
        super(cause);
    }
}