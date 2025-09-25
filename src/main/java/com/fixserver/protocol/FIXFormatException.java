package com.fixserver.protocol;

/**
 * Exception thrown when FIX message formatting fails
 */
public class FIXFormatException extends RuntimeException {
    
    public FIXFormatException(String message) {
        super(message);
    }
    
    public FIXFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}