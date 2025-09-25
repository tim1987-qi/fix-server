package com.fixserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Netty decoder for FIX protocol messages.
 * 
 * This decoder handles the framing of FIX messages by detecting complete
 * messages based on the FIX protocol structure. It accumulates bytes until
 * a complete FIX message is received, then passes it to the next handler.
 * 
 * FIX Message Structure:
 * - BeginString (8=FIX.4.4)
 * - BodyLength (9=<length>)
 * - Message body (various fields)
 * - Checksum (10=<checksum>)
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class FIXMessageDecoder extends ByteToMessageDecoder {
    
    private static final byte SOH = 0x01; // Start of Header character
    private static final String BEGIN_STRING_PREFIX = "8=";
    private static final String BODY_LENGTH_PREFIX = "9=";
    private static final String CHECKSUM_PREFIX = "10=";
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Mark the current reader index
        in.markReaderIndex();
        
        try {
            // Try to decode a complete FIX message
            String message = tryDecodeMessage(in);
            if (message != null) {
                log.debug("Decoded FIX message: {}", message.replace('\u0001', '|'));
                out.add(message);
            } else {
                // Not enough data for a complete message, reset and wait for more
                in.resetReaderIndex();
            }
        } catch (Exception e) {
            log.error("Error decoding FIX message", e);
            // Reset to marked position and skip problematic data
            in.resetReaderIndex();
            in.skipBytes(1); // Skip one byte to avoid infinite loop
        }
    }
    
    /**
     * Attempts to decode a complete FIX message from the buffer.
     * 
     * @param buffer the input buffer
     * @return complete FIX message string, or null if not enough data
     */
    private String tryDecodeMessage(ByteBuf buffer) {
        if (buffer.readableBytes() < 20) { // Minimum FIX message size
            return null;
        }
        
        // Find BeginString field
        int beginStringStart = findField(buffer, BEGIN_STRING_PREFIX);
        if (beginStringStart == -1) {
            return null;
        }
        
        // Find BodyLength field
        int bodyLengthStart = findField(buffer, BODY_LENGTH_PREFIX, beginStringStart);
        if (bodyLengthStart == -1) {
            return null;
        }
        
        // Extract body length value
        int bodyLength = extractBodyLength(buffer, bodyLengthStart);
        if (bodyLength == -1) {
            return null;
        }
        
        // Calculate total message length
        // BeginString + BodyLength fields + Body + Checksum field
        int bodyLengthFieldEnd = findNextSOH(buffer, bodyLengthStart);
        if (bodyLengthFieldEnd == -1) {
            return null;
        }
        
        int expectedMessageEnd = bodyLengthFieldEnd + 1 + bodyLength;
        
        // Check if we have enough data for the complete message
        if (buffer.readableBytes() < expectedMessageEnd - buffer.readerIndex()) {
            return null;
        }
        
        // Verify checksum field is present
        if (!hasChecksumField(buffer, expectedMessageEnd)) {
            return null;
        }
        
        // Find the actual end of the message (after checksum field)
        int checksumEnd = findNextSOH(buffer, expectedMessageEnd);
        if (checksumEnd == -1) {
            return null;
        }
        
        // Extract the complete message
        int messageLength = checksumEnd + 1 - buffer.readerIndex();
        byte[] messageBytes = new byte[messageLength];
        buffer.readBytes(messageBytes);
        
        return new String(messageBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Finds a field prefix in the buffer starting from the given position.
     */
    private int findField(ByteBuf buffer, String prefix) {
        return findField(buffer, prefix, buffer.readerIndex());
    }
    
    /**
     * Finds a field prefix in the buffer starting from the given position.
     */
    private int findField(ByteBuf buffer, String prefix, int startPos) {
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        
        for (int i = startPos; i <= buffer.writerIndex() - prefixBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < prefixBytes.length; j++) {
                if (buffer.getByte(i + j) != prefixBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Extracts the body length value from the BodyLength field.
     */
    private int extractBodyLength(ByteBuf buffer, int bodyLengthStart) {
        int valueStart = bodyLengthStart + BODY_LENGTH_PREFIX.length();
        int valueEnd = findNextSOH(buffer, valueStart);
        
        if (valueEnd == -1) {
            return -1;
        }
        
        try {
            byte[] lengthBytes = new byte[valueEnd - valueStart];
            buffer.getBytes(valueStart, lengthBytes);
            String lengthStr = new String(lengthBytes, StandardCharsets.UTF_8);
            return Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            log.error("Invalid body length format", e);
            return -1;
        }
    }
    
    /**
     * Finds the next SOH character starting from the given position.
     */
    private int findNextSOH(ByteBuf buffer, int startPos) {
        for (int i = startPos; i < buffer.writerIndex(); i++) {
            if (buffer.getByte(i) == SOH) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Checks if there's a checksum field at the expected position.
     */
    private boolean hasChecksumField(ByteBuf buffer, int position) {
        byte[] checksumPrefix = CHECKSUM_PREFIX.getBytes(StandardCharsets.UTF_8);
        
        if (position + checksumPrefix.length > buffer.writerIndex()) {
            return false;
        }
        
        for (int i = 0; i < checksumPrefix.length; i++) {
            if (buffer.getByte(position + i) != checksumPrefix[i]) {
                return false;
            }
        }
        return true;
    }
}