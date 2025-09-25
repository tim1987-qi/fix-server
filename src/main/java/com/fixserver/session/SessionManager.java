package com.fixserver.session;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Central manager for the complete lifecycle of all FIX sessions in the server.
 * 
 * The SessionManager is responsible for:
 * - Creating and destroying FIX sessions
 * - Managing session limits and resource allocation
 * - Monitoring session health and handling timeouts
 * - Providing session statistics and operational metrics
 * - Coordinating graceful shutdown of all sessions
 * 
 * Session Lifecycle Management:
 * 1. Session Creation: Validates parameters and initializes session state
 * 2. Session Monitoring: Tracks active sessions and their health status
 * 3. Timeout Handling: Detects and handles unresponsive sessions
 * 4. Session Cleanup: Properly releases resources when sessions end
 * 
 * Concurrency and Thread Safety:
 * - All operations are thread-safe and can be called concurrently
 * - Asynchronous operations return CompletableFuture for non-blocking execution
 * - Session limits are enforced atomically to prevent race conditions
 * 
 * Resource Management:
 * - Enforces configurable limits on concurrent sessions
 * - Monitors memory and connection usage
 * - Provides graceful degradation under resource pressure
 * 
 * Operational Features:
 * - Real-time session statistics for monitoring
 * - Health checks for operational readiness
 * - Graceful shutdown with proper session cleanup
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public interface SessionManager {
    
    /**
     * Creates a new FIX session with the specified identifiers.
     * 
     * This method performs the following operations:
     * 1. Validates session parameters (non-null, proper format)
     * 2. Checks if session limit would be exceeded
     * 3. Verifies no duplicate session exists with same ID
     * 4. Initializes session state and resources
     * 5. Registers session for monitoring and management
     * 
     * The session is created in DISCONNECTED state and must be explicitly
     * connected and logged on to become active.
     * 
     * @param sessionId unique identifier for the session (typically sender-target combination)
     * @param senderCompId company ID that will appear as sender in outgoing messages
     * @param targetCompId company ID that will appear as target in outgoing messages
     * @return CompletableFuture containing the created session
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SessionLimitExceededException if maximum sessions would be exceeded
     * @throws DuplicateSessionException if session with same ID already exists
     */
    CompletableFuture<FIXSession> createSession(String sessionId, String senderCompId, String targetCompId);
    
    /**
     * Retrieves an existing session by its unique identifier.
     * 
     * This is a fast lookup operation that returns immediately.
     * The returned session may be in any state (connected, disconnected, etc.).
     * 
     * @param sessionId unique identifier of the session to retrieve
     * @return Optional containing the session if found, empty otherwise
     */
    Optional<FIXSession> getSession(String sessionId);
    
    /**
     * Removes a session and cleans up all associated resources.
     * 
     * This method performs graceful session cleanup:
     * 1. Initiates logout if session is currently logged on
     * 2. Waits for logout acknowledgment (with timeout)
     * 3. Closes network connections
     * 4. Releases memory and other resources
     * 5. Removes session from active session tracking
     * 
     * The operation is idempotent - removing a non-existent session is safe.
     * 
     * @param sessionId unique identifier of the session to remove
     * @return CompletableFuture that completes when cleanup is finished
     */
    CompletableFuture<Void> removeSession(String sessionId);
    
    /**
     * Returns a snapshot of all currently active sessions.
     * 
     * Active sessions are those that are not in DISCONNECTED state.
     * The returned list is a snapshot and will not reflect subsequent changes.
     * 
     * @return list of active sessions (may be empty, never null)
     */
    List<FIXSession> getActiveSessions();
    
    /**
     * Handles timeout for a session that has become unresponsive.
     * 
     * This method is typically called by the timeout monitoring system
     * when a session fails to respond to heartbeat or test requests.
     * 
     * Actions taken:
     * 1. Log timeout event for audit trail
     * 2. Attempt graceful disconnect with short timeout
     * 3. Force disconnect if graceful attempt fails
     * 4. Clean up session resources
     * 5. Update session statistics
     * 
     * @param sessionId unique identifier of the timed-out session
     */
    void handleTimeout(String sessionId);
    
    /**
     * Gets the current number of active sessions.
     * 
     * This count includes all sessions that are not in DISCONNECTED state.
     * Used for monitoring, capacity planning, and enforcing session limits.
     * 
     * @return current number of active sessions (0 or positive)
     */
    int getActiveSessionCount();
    
    /**
     * Checks if the configured session limit has been reached.
     * 
     * Used to determine if new session creation requests should be accepted.
     * Takes into account both hard limits and any reserved capacity for
     * administrative or priority connections.
     * 
     * @return true if no more sessions can be created, false otherwise
     */
    boolean isSessionLimitReached();
    
    /**
     * Gets the maximum number of concurrent sessions allowed.
     * 
     * This limit is typically configured based on system capacity,
     * licensing restrictions, or operational policies.
     * 
     * @return maximum concurrent session limit
     */
    int getMaxSessions();
    
    /**
     * Initiates graceful shutdown of all active sessions.
     * 
     * This method is typically called during application shutdown to ensure
     * all sessions are properly closed and resources are cleaned up.
     * 
     * Shutdown process:
     * 1. Stop accepting new sessions
     * 2. Send logout messages to all active sessions
     * 3. Wait for logout acknowledgments (with timeout)
     * 4. Force close any remaining connections
     * 5. Clean up all resources
     * 
     * @return CompletableFuture that completes when all sessions are shut down
     */
    CompletableFuture<Void> shutdownAllSessions();
    
    /**
     * Gets comprehensive statistics about session management.
     * 
     * Provides operational metrics for monitoring, alerting, and capacity planning.
     * Statistics are calculated in real-time and reflect current system state.
     * 
     * @return current session statistics
     */
    SessionStatistics getSessionStatistics();
    
    /**
     * Interface providing comprehensive statistics about session management.
     * 
     * These statistics are essential for:
     * - System monitoring and alerting
     * - Capacity planning and scaling decisions
     * - Performance analysis and optimization
     * - Operational dashboards and reporting
     * 
     * All statistics are calculated in real-time and represent the current
     * state of the system at the time of the call.
     */
    interface SessionStatistics {
        /** Total number of sessions created since server startup */
        int getTotalSessions();
        
        /** Current number of sessions in any non-DISCONNECTED state */
        int getActiveSessions();
        
        /** Current number of sessions in LOGGED_ON state (ready for business) */
        int getLoggedOnSessions();
        
        /** Total number of FIX messages processed across all sessions */
        long getTotalMessagesProcessed();
        
        /** Average message processing rate (messages per second) */
        double getAverageMessageRate();
    }
}