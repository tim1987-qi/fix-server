package com.fixserver.netty;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a FIX session in the Netty-based server.
 * 
 * This class maintains session state, sequence numbers, and connection
 * information for a FIX client connection handled by Netty.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Getter
@Setter
public class NettyFIXSession {

    private final String sessionId;
    private final ChannelHandlerContext channelContext;
    private final LocalDateTime createdTime;

    private boolean loggedOn = false;
    private LocalDateTime lastActivity;
    private final AtomicInteger incomingSeqNum = new AtomicInteger(1);
    private final AtomicInteger outgoingSeqNum = new AtomicInteger(1);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);

    public NettyFIXSession(String sessionId, ChannelHandlerContext channelContext) {
        this.sessionId = sessionId;
        this.channelContext = channelContext;
        this.createdTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Updates the last activity timestamp.
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Gets the next incoming sequence number.
     */
    public int getNextIncomingSeqNum() {
        return incomingSeqNum.getAndIncrement();
    }

    /**
     * Gets the next outgoing sequence number.
     */
    public int getNextOutgoingSeqNum() {
        return outgoingSeqNum.getAndIncrement();
    }

    /**
     * Increments the messages received counter.
     */
    public void incrementMessagesReceived() {
        messagesReceived.incrementAndGet();
    }

    /**
     * Increments the messages sent counter.
     */
    public void incrementMessagesSent() {
        messagesSent.incrementAndGet();
    }

    /**
     * Returns the client's remote address.
     */
    public String getRemoteAddress() {
        return channelContext.channel().remoteAddress().toString();
    }

    /**
     * Returns whether the channel is active.
     */
    public boolean isActive() {
        return channelContext.channel().isActive();
    }

    /**
     * Returns session statistics as a string.
     */
    public String getSessionStats() {
        return String.format("Session[%s] - LoggedOn: %s, Active: %s, MsgRecv: %d, MsgSent: %d, LastActivity: %s",
                sessionId, loggedOn, isActive(), messagesReceived.get(), messagesSent.get(), lastActivity);
    }

    @Override
    public String toString() {
        return String.format("NettyFIXSession{sessionId='%s', loggedOn=%s, active=%s, remote=%s}",
                sessionId, loggedOn, isActive(), getRemoteAddress());
    }
}