package com.fixserver.session;

import com.fixserver.core.FIXMessage;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface representing a FIX session between two trading counterparties.
 * 
 * A FIX session is a logical connection that maintains state between a sender
 * and target.
 * It handles the complete lifecycle of FIX communication including:
 * - Session establishment (logon/logout)
 * - Message sequencing and ordering
 * - Heartbeat monitoring for connection health
 * - Message replay and gap filling
 * - Error handling and recovery
 * 
 * Session State Management:
 * - Each session maintains separate sequence numbers for incoming/outgoing
 * messages
 * - Heartbeat intervals ensure connection liveness
 * - Session state persists across reconnections for message recovery
 * 
 * Thread Safety:
 * - All methods are designed to be thread-safe
 * - Asynchronous operations return CompletableFuture for non-blocking execution
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public interface FIXSession {

    /**
     * Enumeration of possible session states in the FIX session lifecycle.
     * 
     * State Transitions:
     * DISCONNECTED -> CONNECTING -> LOGON_SENT -> LOGGED_ON -> LOGOUT_SENT ->
     * DISCONNECTING -> DISCONNECTED
     * 
     * States can also transition directly to DISCONNECTED from any state in case of
     * errors.
     */
    enum Status {
        /** Session is not connected - initial and final state */
        DISCONNECTED,
        /** Connection is being established but logon not yet sent */
        CONNECTING,
        /** Logon message has been sent, waiting for response */
        LOGON_SENT,
        /** Session is fully established and ready for business messages */
        LOGGED_ON,
        /** Logout message has been sent, waiting for acknowledgment */
        LOGOUT_SENT,
        /** Session is being torn down gracefully */
        DISCONNECTING
    }

    /**
     * Gets the unique identifier for this session.
     * Typically constructed from sender and target company IDs.
     * 
     * @return unique session identifier string
     */
    String getSessionId();

    /**
     * Gets the company ID that identifies this session as the sender.
     * This appears in the SenderCompID field of outgoing messages.
     * 
     * @return sender company identifier
     */
    String getSenderCompId();

    /**
     * Gets the company ID that identifies the counterparty (target).
     * This appears in the TargetCompID field of outgoing messages.
     * 
     * @return target company identifier
     */
    String getTargetCompId();

    /**
     * Gets the current state of the session lifecycle.
     * Used to determine what operations are valid and session health.
     * 
     * @return current session status
     */
    Status getStatus();

    /**
     * Gets the next sequence number expected from the counterparty.
     * Used for gap detection and message ordering validation.
     * 
     * @return next expected incoming sequence number
     */
    int getNextIncomingSequenceNumber();

    /**
     * Gets the next sequence number to use for outgoing messages.
     * Automatically incremented when messages are sent.
     * 
     * @return next outgoing sequence number
     */
    int getNextOutgoingSequenceNumber();

    /**
     * Gets the timestamp of the last heartbeat received or sent.
     * Used for connection health monitoring and timeout detection.
     * 
     * @return last heartbeat timestamp or null if none recorded
     */
    LocalDateTime getLastHeartbeat();

    /**
     * Gets the timestamp when the current session was established.
     * Used for session duration calculations and statistics.
     * 
     * @return session start time or null if not started
     */
    LocalDateTime getSessionStartTime();

    /**
     * Processes an incoming FIX message asynchronously.
     * Handles sequence number validation, message routing, and protocol logic.
     * 
     * Processing includes:
     * - Sequence number validation and gap detection
     * - Message type-specific handling (logon, heartbeat, business messages)
     * - Automatic responses (heartbeat replies, sequence resets)
     * - Message persistence for audit and replay
     * 
     * @param message the incoming FIX message to process
     * @return CompletableFuture that completes when processing is done
     */
    CompletableFuture<Void> processMessage(FIXMessage message);

    /**
     * Sends a FIX message to the counterparty asynchronously.
     * Automatically sets session-specific fields and handles sequencing.
     * 
     * The method will:
     * - Set SenderCompID, TargetCompID, and MsgSeqNum fields
     * - Update SendingTime to current timestamp
     * - Store message for audit and potential replay
     * - Format message and send via network connection
     * 
     * @param message the FIX message to send
     * @return CompletableFuture that completes when message is sent
     */
    CompletableFuture<Void> sendMessage(FIXMessage message);

    /**
     * Handles heartbeat processing for connection health monitoring.
     * Sends heartbeat messages when required based on configured intervals.
     * 
     * Called periodically by the heartbeat manager to maintain session liveness.
     * Will send heartbeat if sufficient time has passed since last activity.
     */
    void handleHeartbeat();

    /**
     * Initiates graceful session disconnect asynchronously.
     * Sends logout message and transitions session to disconnected state.
     * 
     * The disconnect process:
     * - Sends logout message if session is logged on
     * - Waits for logout acknowledgment (with timeout)
     * - Closes network connection
     * - Updates session state to DISCONNECTED
     * 
     * @return CompletableFuture that completes when disconnect is finished
     */
    CompletableFuture<Void> disconnect();

    /**
     * Checks if the session is currently active and able to process messages.
     * A session is considered active if it's in LOGGED_ON or CONNECTING state.
     * 
     * @return true if session can process business messages, false otherwise
     */
    boolean isActive();

    /**
     * Gets the configured heartbeat interval for this session in seconds.
     * Determines how frequently heartbeat messages should be exchanged.
     * 
     * @return heartbeat interval in seconds
     */
    int getHeartbeatInterval();
}