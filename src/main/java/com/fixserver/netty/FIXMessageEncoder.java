package com.fixserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Netty encoder for FIX protocol messages.
 * 
 * This encoder converts FIX message strings into bytes for transmission
 * over the network. It handles the proper encoding and ensures the
 * message is written to the channel buffer correctly.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class FIXMessageEncoder extends MessageToByteEncoder<String> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        if (msg == null || msg.isEmpty()) {
            log.warn("Attempted to encode null or empty FIX message");
            return;
        }
        
        try {
            // Convert string to bytes using UTF-8 encoding
            byte[] messageBytes = msg.getBytes(StandardCharsets.UTF_8);
            
            // Write bytes to output buffer
            out.writeBytes(messageBytes);
            
            log.debug("Encoded FIX message: {} ({} bytes)", 
                    msg.replace('\u0001', '|'), messageBytes.length);
            
        } catch (Exception e) {
            log.error("Error encoding FIX message: {}", msg.replace('\u0001', '|'), e);
            throw e;
        }
    }
}