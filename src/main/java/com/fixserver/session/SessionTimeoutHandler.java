package com.fixserver.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles session timeouts and cleanup operations
 */
@Slf4j
@Component
public class SessionTimeoutHandler {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, TimeoutTask> timeoutTasks = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Configuration
    private final int timeoutCheckInterval = 30; // seconds
    private final int defaultSessionTimeout = 120; // seconds
    
    /**
     * Start the timeout handler
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("Starting SessionTimeoutHandler");
            
            scheduler.scheduleAtFixedRate(
                this::checkTimeouts,
                timeoutCheckInterval,
                timeoutCheckInterval,
                TimeUnit.SECONDS
            );
        }
    }
    
    /**
     * Stop the timeout handler
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping SessionTimeoutHandler");
            
            timeoutTasks.clear();
            scheduler.shutdown();
            
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Register a session for timeout monitoring
     */
    public void registerSession(String sessionId, int timeoutSeconds, TimeoutCallback callback) {
        TimeoutTask task = new TimeoutTask(sessionId, timeoutSeconds, callback);
        timeoutTasks.put(sessionId, task);
        
        log.debug("Registered session {} for timeout monitoring ({}s)", sessionId, timeoutSeconds);
    }
    
    /**
     * Register a session with default timeout
     */
    public void registerSession(String sessionId, TimeoutCallback callback) {
        registerSession(sessionId, defaultSessionTimeout, callback);
    }
    
    /**
     * Unregister a session from timeout monitoring
     */
    public void unregisterSession(String sessionId) {
        TimeoutTask task = timeoutTasks.remove(sessionId);
        if (task != null) {
            log.debug("Unregistered session {} from timeout monitoring", sessionId);
        }
    }
    
    /**
     * Update the last activity time for a session
     */
    public void updateActivity(String sessionId) {
        TimeoutTask task = timeoutTasks.get(sessionId);
        if (task != null) {
            task.updateLastActivity();
            log.debug("Updated activity for session {}", sessionId);
        }
    }
    
    /**
     * Check if a session has timed out
     */
    public boolean isTimedOut(String sessionId) {
        TimeoutTask task = timeoutTasks.get(sessionId);
        return task != null && task.isTimedOut();
    }
    
    /**
     * Get the time until timeout for a session
     */
    public long getTimeUntilTimeout(String sessionId) {
        TimeoutTask task = timeoutTasks.get(sessionId);
        return task != null ? task.getTimeUntilTimeout() : -1;
    }
    
    /**
     * Manually trigger timeout for a session
     */
    public void triggerTimeout(String sessionId) {
        TimeoutTask task = timeoutTasks.get(sessionId);
        if (task != null) {
            task.triggerTimeout();
        }
    }
    
    /**
     * Get timeout statistics for a session
     */
    public TimeoutStats getTimeoutStats(String sessionId) {
        TimeoutTask task = timeoutTasks.get(sessionId);
        return task != null ? task.getStats() : null;
    }
    
    private void checkTimeouts() {
        try {
            for (TimeoutTask task : timeoutTasks.values()) {
                if (task.isTimedOut()) {
                    task.handleTimeout();
                }
            }
        } catch (Exception e) {
            log.error("Error during timeout check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Callback interface for timeout events
     */
    public interface TimeoutCallback {
        void onSessionTimeout(String sessionId, TimeoutReason reason);
    }
    
    /**
     * Timeout reason enumeration
     */
    public enum TimeoutReason {
        INACTIVITY,
        HEARTBEAT_TIMEOUT,
        MANUAL_TRIGGER,
        LOGON_TIMEOUT,
        LOGOUT_TIMEOUT
    }
    
    /**
     * Timeout statistics
     */
    public static class TimeoutStats {
        private final String sessionId;
        private final LocalDateTime lastActivity;
        private final int timeoutSeconds;
        private final long timeUntilTimeout;
        private final boolean isTimedOut;
        private final int timeoutCount;
        
        public TimeoutStats(String sessionId, LocalDateTime lastActivity, int timeoutSeconds,
                           long timeUntilTimeout, boolean isTimedOut, int timeoutCount) {
            this.sessionId = sessionId;
            this.lastActivity = lastActivity;
            this.timeoutSeconds = timeoutSeconds;
            this.timeUntilTimeout = timeUntilTimeout;
            this.isTimedOut = isTimedOut;
            this.timeoutCount = timeoutCount;
        }
        
        public String getSessionId() { return sessionId; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public long getTimeUntilTimeout() { return timeUntilTimeout; }
        public boolean isTimedOut() { return isTimedOut; }
        public int getTimeoutCount() { return timeoutCount; }
    }
    
    /**
     * Internal timeout task for individual sessions
     */
    private static class TimeoutTask {
        private final String sessionId;
        private final int timeoutSeconds;
        private final TimeoutCallback callback;
        private volatile LocalDateTime lastActivity;
        private volatile boolean timedOut = false;
        private volatile int timeoutCount = 0;
        
        public TimeoutTask(String sessionId, int timeoutSeconds, TimeoutCallback callback) {
            this.sessionId = sessionId;
            this.timeoutSeconds = timeoutSeconds;
            this.callback = callback;
            this.lastActivity = LocalDateTime.now();
        }
        
        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
            this.timedOut = false;
        }
        
        public boolean isTimedOut() {
            if (timedOut) {
                return true;
            }
            
            LocalDateTime now = LocalDateTime.now();
            return lastActivity.plusSeconds(timeoutSeconds).isBefore(now);
        }
        
        public long getTimeUntilTimeout() {
            if (timedOut) {
                return 0;
            }
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime timeoutTime = lastActivity.plusSeconds(timeoutSeconds);
            
            if (timeoutTime.isBefore(now)) {
                return 0;
            }
            
            return java.time.Duration.between(now, timeoutTime).getSeconds();
        }
        
        public void triggerTimeout() {
            this.timedOut = true;
            handleTimeout();
        }
        
        public void handleTimeout() {
            if (!timedOut) {
                timedOut = true;
                timeoutCount++;
                
                try {
                    log.warn("Session {} timed out after {} seconds of inactivity", 
                            sessionId, timeoutSeconds);
                    callback.onSessionTimeout(sessionId, TimeoutReason.INACTIVITY);
                } catch (Exception e) {
                    log.error("Error handling timeout for session {}: {}", 
                             sessionId, e.getMessage(), e);
                }
            }
        }
        
        public TimeoutStats getStats() {
            return new TimeoutStats(
                sessionId,
                lastActivity,
                timeoutSeconds,
                getTimeUntilTimeout(),
                isTimedOut(),
                timeoutCount
            );
        }
    }
}