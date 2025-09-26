package com.fixserver.session;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages heartbeat monitoring and timeout detection for FIX sessions
 */
@Slf4j
@Component
public class HeartbeatManager {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, HeartbeatMonitor> monitors = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Configuration
    private final int heartbeatCheckInterval = 10; // seconds
    private final double timeoutMultiplier = 2.0; // timeout = heartbeat_interval * multiplier

    /**
     * Start the heartbeat manager
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("Starting HeartbeatManager");

            // Schedule periodic heartbeat checks
            scheduler.scheduleAtFixedRate(
                    this::checkHeartbeats,
                    heartbeatCheckInterval,
                    heartbeatCheckInterval,
                    TimeUnit.SECONDS);

            // Schedule periodic timeout checks
            scheduler.scheduleAtFixedRate(
                    this::checkTimeouts,
                    heartbeatCheckInterval,
                    heartbeatCheckInterval,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the heartbeat manager
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping HeartbeatManager");

            monitors.clear();
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
     * Register a session for heartbeat monitoring
     */
    public void registerSession(FIXSession session, HeartbeatCallback callback) {
        String sessionId = session.getSessionId();

        HeartbeatMonitor monitor = new HeartbeatMonitor(session, callback);
        monitors.put(sessionId, monitor);

        log.debug("Registered session {} for heartbeat monitoring", sessionId);
    }

    /**
     * Unregister a session from heartbeat monitoring
     */
    public void unregisterSession(String sessionId) {
        HeartbeatMonitor monitor = monitors.remove(sessionId);
        if (monitor != null) {
            log.debug("Unregistered session {} from heartbeat monitoring", sessionId);
        }
    }

    /**
     * Update the last heartbeat time for a session
     */
    public void updateHeartbeat(String sessionId) {
        HeartbeatMonitor monitor = monitors.get(sessionId);
        if (monitor != null) {
            monitor.updateLastHeartbeat();
            log.debug("Updated heartbeat for session {}", sessionId);
        }
    }

    /**
     * Check if a session needs to send a heartbeat
     */
    public boolean shouldSendHeartbeat(String sessionId) {
        HeartbeatMonitor monitor = monitors.get(sessionId);
        return monitor != null && monitor.shouldSendHeartbeat();
    }

    /**
     * Check if a session has timed out
     */
    public boolean isSessionTimedOut(String sessionId) {
        HeartbeatMonitor monitor = monitors.get(sessionId);
        return monitor != null && monitor.isTimedOut();
    }

    /**
     * Get heartbeat statistics for a session
     */
    public HeartbeatStats getHeartbeatStats(String sessionId) {
        HeartbeatMonitor monitor = monitors.get(sessionId);
        return monitor != null ? monitor.getStats() : null;
    }

    /**
     * Send a test request to a session
     */
    public void sendTestRequest(String sessionId) {
        HeartbeatMonitor monitor = monitors.get(sessionId);
        if (monitor != null) {
            monitor.sendTestRequest();
        }
    }

    private void checkHeartbeats() {
        try {
            for (HeartbeatMonitor monitor : monitors.values()) {
                if (monitor.shouldSendHeartbeat()) {
                    monitor.sendHeartbeat();
                }
            }
        } catch (Exception e) {
            log.error("Error during heartbeat check: {}", e.getMessage(), e);
        }
    }

    private void checkTimeouts() {
        try {
            for (HeartbeatMonitor monitor : monitors.values()) {
                if (monitor.shouldSendTestRequest()) {
                    monitor.sendTestRequest();
                } else if (monitor.isTimedOut()) {
                    monitor.handleTimeout();
                }
            }
        } catch (Exception e) {
            log.error("Error during timeout check: {}", e.getMessage(), e);
        }
    }

    /**
     * Callback interface for heartbeat events
     */
    public interface HeartbeatCallback {
        void onHeartbeatRequired(String sessionId);

        void onTestRequestRequired(String sessionId, String testReqId);

        void onSessionTimeout(String sessionId);
    }

    /**
     * Heartbeat statistics
     */
    public static class HeartbeatStats {
        private final LocalDateTime lastHeartbeat;
        private final LocalDateTime lastTestRequest;
        private final long heartbeatsSent;
        private final long testRequestsSent;
        private final boolean isTimedOut;

        public HeartbeatStats(LocalDateTime lastHeartbeat, LocalDateTime lastTestRequest,
                long heartbeatsSent, long testRequestsSent, boolean isTimedOut) {
            this.lastHeartbeat = lastHeartbeat;
            this.lastTestRequest = lastTestRequest;
            this.heartbeatsSent = heartbeatsSent;
            this.testRequestsSent = testRequestsSent;
            this.isTimedOut = isTimedOut;
        }

        public LocalDateTime getLastHeartbeat() {
            return lastHeartbeat;
        }

        public LocalDateTime getLastTestRequest() {
            return lastTestRequest;
        }

        public long getHeartbeatsSent() {
            return heartbeatsSent;
        }

        public long getTestRequestsSent() {
            return testRequestsSent;
        }

        public boolean isTimedOut() {
            return isTimedOut;
        }
    }

    /**
     * Internal monitor for individual sessions
     */
    private static class HeartbeatMonitor {
        private final FIXSession session;
        private final HeartbeatCallback callback;
        private volatile LocalDateTime lastHeartbeat;
        private volatile LocalDateTime lastTestRequest;
        private volatile LocalDateTime lastHeartbeatSent;
        private volatile long heartbeatsSent = 0;
        private volatile long testRequestsSent = 0;
        private volatile String pendingTestReqId;

        public HeartbeatMonitor(FIXSession session, HeartbeatCallback callback) {
            this.session = session;
            this.callback = callback;
            this.lastHeartbeat = LocalDateTime.now();
            this.lastHeartbeatSent = LocalDateTime.now();
        }

        public void updateLastHeartbeat() {
            this.lastHeartbeat = LocalDateTime.now();
            this.pendingTestReqId = null; // Clear pending test request
        }

        public boolean shouldSendHeartbeat() {
            if (!session.isActive()) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            int heartbeatInterval = session.getHeartbeatInterval();

            return lastHeartbeatSent.plusSeconds(heartbeatInterval).isBefore(now);
        }

        public boolean shouldSendTestRequest() {
            if (!session.isActive() || pendingTestReqId != null) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            int heartbeatInterval = session.getHeartbeatInterval();

            // Send test request if no heartbeat received for 1.5x heartbeat interval
            return lastHeartbeat.plusSeconds((long) (heartbeatInterval * 1.5)).isBefore(now);
        }

        public boolean isTimedOut() {
            if (!session.isActive()) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            int heartbeatInterval = session.getHeartbeatInterval();

            // Consider timed out if no heartbeat for 2x heartbeat interval
            // and we have a pending test request
            return pendingTestReqId != null &&
                    lastHeartbeat.plusSeconds((long) (heartbeatInterval * 2.0)).isBefore(now);
        }

        public void sendHeartbeat() {
            try {
                callback.onHeartbeatRequired(session.getSessionId());
                lastHeartbeatSent = LocalDateTime.now();
                heartbeatsSent++;

                log.debug("Heartbeat sent for session {}", session.getSessionId());
            } catch (Exception e) {
                log.error("Error sending heartbeat for session {}: {}",
                        session.getSessionId(), e.getMessage(), e);
            }
        }

        public void sendTestRequest() {
            try {
                pendingTestReqId = generateTestReqId();
                callback.onTestRequestRequired(session.getSessionId(), pendingTestReqId);
                lastTestRequest = LocalDateTime.now();
                testRequestsSent++;

                log.debug("Test request sent for session {}: {}", session.getSessionId(), pendingTestReqId);
            } catch (Exception e) {
                log.error("Error sending test request for session {}: {}",
                        session.getSessionId(), e.getMessage(), e);
            }
        }

        public void handleTimeout() {
            try {
                log.warn("Session {} timed out", session.getSessionId());
                callback.onSessionTimeout(session.getSessionId());
            } catch (Exception e) {
                log.error("Error handling timeout for session {}: {}",
                        session.getSessionId(), e.getMessage(), e);
            }
        }

        public HeartbeatStats getStats() {
            return new HeartbeatStats(
                    lastHeartbeat,
                    lastTestRequest,
                    heartbeatsSent,
                    testRequestsSent,
                    isTimedOut());
        }

        private String generateTestReqId() {
            return "TEST_" + System.currentTimeMillis();
        }
    }
}