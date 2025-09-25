package com.fixserver.protocol;

/**
 * Exception thrown when FIX message parsing fails
 */
public class FIXParseException extends RuntimeException {
    
    public FIXParseException(String message) {
        super(message);
    }
    
    public FIXParseException(String message, Throwable cause) {
        super(message, cause);
    }
}