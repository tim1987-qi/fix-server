package com.fixserver.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Performance optimization utilities and monitoring for the FIX server.
 * 
 * This class provides:
 * - Message processing performance metrics
 * - Memory optimization utilities
 * - Connection pool optimization
 * - Garbage collection monitoring
 * - Performance tuning recommendations
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class PerformanceOptimizer {
    
    // Performance metrics
    private final LongAdder totalMessagesProcessed = new LongAdder();
    private final LongAdder totalBytesProcessed = new LongAdder();
    private final AtomicLong maxProcessingTime = new AtomicLong(0);
    private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
    private final LongAdder totalProcessingTime = new LongAdder();
    
    // Connection metrics
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong maxConcurrentConnections = new AtomicLong(0);
    private final LongAdder totalConnectionsCreated = new LongAdder();
    
    // Memory optimization - Enhanced object pools
    private final ConcurrentHashMap<String, Object> objectPool = new ConcurrentHashMap<>();
    private final ThreadLocal<StringBuilder> stringBuilderPool = ThreadLocal.withInitial(() -> new StringBuilder(2048));
    private final ThreadLocal<byte[]> byteBufferPool = ThreadLocal.withInitial(() -> new byte[8192]);
    
    // Integration with optimized components
    @Autowired(required = false)
    private HighPerformanceMessageParser optimizedParser;
    
    @Autowired(required = false)
    private AsyncMessageStore asyncStore;
    
    @Autowired(required = false)
    private JVMOptimizationConfig jvmConfig;
    
    // Timing utilities
    private final LocalDateTime startTime = LocalDateTime.now();
    
    /**
     * Record message processing metrics.
     * 
     * @param processingTimeNanos processing time in nanoseconds
     * @param messageSize message size in bytes
     */
    public void recordMessageProcessing(long processingTimeNanos, int messageSize) {
        totalMessagesProcessed.increment();
        totalBytesProcessed.add(messageSize);
        totalProcessingTime.add(processingTimeNanos);
        
        // Update min/max processing times
        updateMinProcessingTime(processingTimeNanos);
        updateMaxProcessingTime(processingTimeNanos);
        
        // Log slow messages (> 10ms)
        if (processingTimeNanos > 10_000_000) {
            log.warn("Slow message processing detected: {}ms for {} bytes", 
                    processingTimeNanos / 1_000_000, messageSize);
        }
    }
    
    /**
     * Record connection metrics.
     */
    public void recordConnectionCreated() {
        long current = activeConnections.incrementAndGet();
        totalConnectionsCreated.increment();
        
        // Update max concurrent connections
        long max = maxConcurrentConnections.get();
        while (current > max && !maxConcurrentConnections.compareAndSet(max, current)) {
            max = maxConcurrentConnections.get();
        }
    }
    
    /**
     * Record connection closure.
     */
    public void recordConnectionClosed() {
        activeConnections.decrementAndGet();
    }
    
    /**
     * Get a reusable StringBuilder for string operations.
     * This reduces garbage collection pressure from frequent string concatenations.
     * 
     * @return thread-local StringBuilder instance
     */
    public StringBuilder getStringBuilder() {
        StringBuilder sb = stringBuilderPool.get();
        sb.setLength(0); // Clear previous content
        return sb;
    }
    
    /**
     * Get a reusable byte buffer for I/O operations.
     * This reduces memory allocation for temporary byte operations.
     * 
     * @return thread-local byte array
     */
    public byte[] getByteBuffer() {
        return byteBufferPool.get();
    }
    
    /**
     * Get optimized message parser if available
     */
    public HighPerformanceMessageParser getOptimizedParser() {
        return optimizedParser;
    }
    
    /**
     * Check if optimized components are available
     */
    public boolean hasOptimizedComponents() {
        return optimizedParser != null && asyncStore != null;
    }
    
    /**
     * Get performance statistics.
     * 
     * @return current performance metrics
     */
    public PerformanceStats getPerformanceStats() {
        long totalMessages = totalMessagesProcessed.sum();
        long totalBytes = totalBytesProcessed.sum();
        long totalTime = totalProcessingTime.sum();
        
        double avgProcessingTime = totalMessages > 0 ? (double) totalTime / totalMessages / 1_000_000 : 0;
        double throughputMps = calculateThroughput(totalMessages);
        double throughputMbps = calculateThroughput(totalBytes) / (1024 * 1024);
        
        return new PerformanceStats(
            totalMessages,
            totalBytes,
            avgProcessingTime,
            minProcessingTime.get() / 1_000_000.0,
            maxProcessingTime.get() / 1_000_000.0,
            throughputMps,
            throughputMbps,
            activeConnections.get(),
            maxConcurrentConnections.get(),
            totalConnectionsCreated.sum()
        );
    }
    
    /**
     * Generate performance optimization recommendations.
     * 
     * @return list of optimization recommendations
     */
    public java.util.List<String> getOptimizationRecommendations() {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        PerformanceStats stats = getPerformanceStats();
        
        // Check processing time
        if (stats.getAvgProcessingTimeMs() > 5.0) {
            recommendations.add("Average processing time is high (" + 
                String.format("%.2f", stats.getAvgProcessingTimeMs()) + "ms). " +
                "Consider optimizing message parsing or validation logic.");
        }
        
        // Check max processing time
        if (stats.getMaxProcessingTimeMs() > 50.0) {
            recommendations.add("Maximum processing time is very high (" + 
                String.format("%.2f", stats.getMaxProcessingTimeMs()) + "ms). " +
                "Investigate slow message processing paths.");
        }
        
        // Check throughput
        if (stats.getThroughputMps() < 1000 && stats.getTotalMessages() > 1000) {
            recommendations.add("Message throughput is low (" + 
                String.format("%.0f", stats.getThroughputMps()) + " msg/sec). " +
                "Consider using Netty server for better performance.");
        }
        
        // Check connection usage
        if (stats.getMaxConcurrentConnections() > 100) {
            recommendations.add("High concurrent connection count (" + 
                stats.getMaxConcurrentConnections() + "). " +
                "Consider implementing connection pooling or rate limiting.");
        }
        
        // Memory recommendations
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory;
        
        if (memoryUsage > 0.8) {
            recommendations.add("High memory usage (" + 
                String.format("%.1f", memoryUsage * 100) + "%). " +
                "Consider increasing heap size or optimizing object allocation.");
        }
        
        // Check if optimized components are being used
        if (!hasOptimizedComponents()) {
            recommendations.add("Enable optimized components: OptimizedFIXMessage, HighPerformanceMessageParser, AsyncMessageStore");
        }
        
        // JVM recommendations
        if (jvmConfig != null) {
            recommendations.addAll(jvmConfig.getJVMTuningRecommendations());
        }
        
        // Async store recommendations
        if (asyncStore != null) {
            AsyncMessageStore.AsyncStoreStats storeStats = asyncStore.getStats();
            if (storeStats.getQueueOverflows() > 0) {
                recommendations.add("Message store queue overflows detected (" + storeStats.getQueueOverflows() + 
                        "). Consider increasing queue size or optimizing storage performance.");
            }
        }
        
        // Parser recommendations
        if (optimizedParser != null) {
            HighPerformanceMessageParser.ParsingStats parsingStats = optimizedParser.getParsingStats();
            if (parsingStats.getAvgTimeMs() > 0.1) {
                recommendations.add("Message parsing is slower than optimal (" + 
                        String.format("%.3f", parsingStats.getAvgTimeMs()) + "ms avg). " +
                        "Consider message format optimization or hardware upgrade.");
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Performance looks excellent! All optimizations are working well.");
        }
        
        return recommendations;
    }
    
    /**
     * Reset all performance metrics.
     */
    public void resetMetrics() {
        totalMessagesProcessed.reset();
        totalBytesProcessed.reset();
        totalProcessingTime.reset();
        maxProcessingTime.set(0);
        minProcessingTime.set(Long.MAX_VALUE);
        totalConnectionsCreated.reset();
        maxConcurrentConnections.set(activeConnections.get());
        
        log.info("Performance metrics reset");
    }
    
    /**
     * Log current performance summary.
     */
    public void logPerformanceSummary() {
        PerformanceStats stats = getPerformanceStats();
        
        log.info("=== FIX Server Performance Summary ===");
        log.info("Total Messages Processed: {}", stats.getTotalMessages());
        log.info("Total Bytes Processed: {} MB", stats.getTotalBytes() / (1024 * 1024));
        log.info("Average Processing Time: {:.2f} ms", stats.getAvgProcessingTimeMs());
        log.info("Min/Max Processing Time: {:.2f}/{:.2f} ms", 
                stats.getMinProcessingTimeMs(), stats.getMaxProcessingTimeMs());
        log.info("Message Throughput: {:.0f} msg/sec", stats.getThroughputMps());
        log.info("Data Throughput: {:.2f} MB/sec", stats.getThroughputMbps());
        log.info("Active Connections: {}", stats.getActiveConnections());
        log.info("Max Concurrent Connections: {}", stats.getMaxConcurrentConnections());
        log.info("Total Connections Created: {}", stats.getTotalConnectionsCreated());
        
        // Log recommendations
        java.util.List<String> recommendations = getOptimizationRecommendations();
        log.info("=== Optimization Recommendations ===");
        for (String recommendation : recommendations) {
            log.info("- {}", recommendation);
        }
    }
    
    private void updateMinProcessingTime(long processingTime) {
        long current = minProcessingTime.get();
        while (processingTime < current && !minProcessingTime.compareAndSet(current, processingTime)) {
            current = minProcessingTime.get();
        }
    }
    
    private void updateMaxProcessingTime(long processingTime) {
        long current = maxProcessingTime.get();
        while (processingTime > current && !maxProcessingTime.compareAndSet(current, processingTime)) {
            current = maxProcessingTime.get();
        }
    }
    
    private double calculateThroughput(long totalItems) {
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        long uptimeSeconds = uptime.getSeconds();
        return uptimeSeconds > 0 ? (double) totalItems / uptimeSeconds : 0;
    }
    
    /**
     * Performance statistics data class.
     */
    public static class PerformanceStats {
        private final long totalMessages;
        private final long totalBytes;
        private final double avgProcessingTimeMs;
        private final double minProcessingTimeMs;
        private final double maxProcessingTimeMs;
        private final double throughputMps;
        private final double throughputMbps;
        private final long activeConnections;
        private final long maxConcurrentConnections;
        private final long totalConnectionsCreated;
        
        public PerformanceStats(long totalMessages, long totalBytes, double avgProcessingTimeMs,
                               double minProcessingTimeMs, double maxProcessingTimeMs,
                               double throughputMps, double throughputMbps,
                               long activeConnections, long maxConcurrentConnections,
                               long totalConnectionsCreated) {
            this.totalMessages = totalMessages;
            this.totalBytes = totalBytes;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.minProcessingTimeMs = minProcessingTimeMs;
            this.maxProcessingTimeMs = maxProcessingTimeMs;
            this.throughputMps = throughputMps;
            this.throughputMbps = throughputMbps;
            this.activeConnections = activeConnections;
            this.maxConcurrentConnections = maxConcurrentConnections;
            this.totalConnectionsCreated = totalConnectionsCreated;
        }
        
        // Getters
        public long getTotalMessages() { return totalMessages; }
        public long getTotalBytes() { return totalBytes; }
        public double getAvgProcessingTimeMs() { return avgProcessingTimeMs; }
        public double getMinProcessingTimeMs() { return minProcessingTimeMs; }
        public double getMaxProcessingTimeMs() { return maxProcessingTimeMs; }
        public double getThroughputMps() { return throughputMps; }
        public double getThroughputMbps() { return throughputMbps; }
        public long getActiveConnections() { return activeConnections; }
        public long getMaxConcurrentConnections() { return maxConcurrentConnections; }
        public long getTotalConnectionsCreated() { return totalConnectionsCreated; }
    }
}