package com.fixserver.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * JVM optimization configuration and monitoring for high-performance FIX server.
 * 
 * This component provides:
 * - JVM parameter validation and recommendations
 * - Garbage collection monitoring and tuning
 * - Memory allocation optimization
 * - JIT compilation monitoring
 * - NUMA awareness configuration
 * 
 * Recommended JVM Parameters for Production:
 * -server
 * -Xms4g -Xmx4g
 * -XX:+UseG1GC
 * -XX:MaxGCPauseMillis=10
 * -XX:+UseStringDeduplication
 * -XX:+OptimizeStringConcat
 * -XX:+UseFastAccessorMethods
 * -XX:+AggressiveOpts
 * -XX:+UseCompressedOops
 * -XX:+UseCompressedClassPointers
 * -Djava.net.preferIPv4Stack=true
 * -XX:+UnlockExperimentalVMOptions
 * -XX:+UseJVMCICompiler (if using GraalVM)
 * 
 * @author FIX Server Performance Team
 * @version 2.0
 * @since 2.0
 */
@Slf4j
@Component
public class JVMOptimizationConfig {
    
    private static final String[] RECOMMENDED_JVM_ARGS = {
            "-server",
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-XX:+OptimizeStringConcat",
            "-XX:+UseFastAccessorMethods",
            "-XX:+UseCompressedOops"
    };
    
    private static final String[] PERFORMANCE_JVM_ARGS = {
            "-XX:+AggressiveOpts",
            "-XX:+UseBiasedLocking",
            "-XX:+DoEscapeAnalysis",
            "-XX:+EliminateAllocations",
            "-XX:+UseFastJNIAccessors"
    };
    
    private MemoryMXBean memoryBean;
    private RuntimeMXBean runtimeBean;
    private MBeanServer mbeanServer;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("Initializing JVM optimization configuration...");
        
        // Initialize MBeans
        memoryBean = ManagementFactory.getMemoryMXBean();
        runtimeBean = ManagementFactory.getRuntimeMXBean();
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        // Log JVM information
        logJVMInformation();
        
        // Validate JVM parameters
        validateJVMParameters();
        
        // Configure JVM optimizations
        configureOptimizations();
        
        // Start monitoring
        startPerformanceMonitoring();
        
        log.info("JVM optimization configuration completed");
    }
    
    /**
     * Log comprehensive JVM information
     */
    private void logJVMInformation() {
        log.info("=== JVM Information ===");
        log.info("JVM Name: {}", runtimeBean.getVmName());
        log.info("JVM Version: {}", runtimeBean.getVmVersion());
        log.info("JVM Vendor: {}", runtimeBean.getVmVendor());
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Java Vendor: {}", System.getProperty("java.vendor"));
        
        // Memory information
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        
        log.info("Max Memory: {} MB", maxMemory / 1024 / 1024);
        log.info("Total Memory: {} MB", totalMemory / 1024 / 1024);
        log.info("Free Memory: {} MB", freeMemory / 1024 / 1024);
        log.info("Used Memory: {} MB", (totalMemory - freeMemory) / 1024 / 1024);
        
        // CPU information
        log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        
        // Uptime
        log.info("JVM Uptime: {} ms", runtimeBean.getUptime());
    }
    
    /**
     * Validate current JVM parameters against recommendations
     */
    private void validateJVMParameters() {
        log.info("=== JVM Parameter Validation ===");
        
        List<String> inputArguments = runtimeBean.getInputArguments();
        log.info("Current JVM Arguments: {}", inputArguments);
        
        // Check for recommended parameters
        for (String recommendedArg : RECOMMENDED_JVM_ARGS) {
            boolean found = inputArguments.stream().anyMatch(arg -> arg.contains(recommendedArg));
            if (found) {
                log.info("✓ Found recommended parameter: {}", recommendedArg);
            } else {
                log.warn("✗ Missing recommended parameter: {}", recommendedArg);
            }
        }
        
        // Check for performance parameters
        for (String perfArg : PERFORMANCE_JVM_ARGS) {
            boolean found = inputArguments.stream().anyMatch(arg -> arg.contains(perfArg));
            if (found) {
                log.info("✓ Found performance parameter: {}", perfArg);
            } else {
                log.info("○ Optional performance parameter not set: {}", perfArg);
            }
        }
        
        // Specific validations
        validateGarbageCollector();
        validateMemorySettings();
        validateCompilerSettings();
    }
    
    /**
     * Validate garbage collector settings
     */
    private void validateGarbageCollector() {
        List<String> inputArguments = runtimeBean.getInputArguments();
        
        boolean hasG1GC = inputArguments.stream().anyMatch(arg -> arg.contains("UseG1GC"));
        boolean hasParallelGC = inputArguments.stream().anyMatch(arg -> arg.contains("UseParallelGC"));
        boolean hasConcMarkSweepGC = inputArguments.stream().anyMatch(arg -> arg.contains("UseConcMarkSweepGC"));
        
        if (hasG1GC) {
            log.info("✓ Using G1GC (recommended for low-latency applications)");
            
            // Check G1 specific settings
            boolean hasMaxGCPause = inputArguments.stream().anyMatch(arg -> arg.contains("MaxGCPauseMillis"));
            if (hasMaxGCPause) {
                log.info("✓ MaxGCPauseMillis is set");
            } else {
                log.warn("Consider setting -XX:MaxGCPauseMillis=10 for ultra-low latency");
            }
            
        } else if (hasParallelGC) {
            log.warn("Using Parallel GC - consider switching to G1GC for better latency");
        } else if (hasConcMarkSweepGC) {
            log.warn("Using CMS GC - consider switching to G1GC (CMS is deprecated)");
        } else {
            log.warn("No explicit GC specified - JVM will use default");
        }
    }
    
    /**
     * Validate memory settings
     */
    private void validateMemorySettings() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        
        // Check if Xms and Xmx are equal (recommended for production)
        if (maxMemory == totalMemory) {
            log.info("✓ Xms and Xmx are equal (good for production)");
        } else {
            log.warn("Consider setting Xms equal to Xmx to avoid dynamic heap expansion");
        }
        
        // Check heap size
        long heapSizeMB = maxMemory / 1024 / 1024;
        if (heapSizeMB < 1024) {
            log.warn("Heap size is small ({} MB) - consider increasing for production", heapSizeMB);
        } else if (heapSizeMB > 8192) {
            log.warn("Large heap size ({} MB) - ensure GC tuning is appropriate", heapSizeMB);
        } else {
            log.info("✓ Heap size looks reasonable: {} MB", heapSizeMB);
        }
    }
    
    /**
     * Validate compiler settings
     */
    private void validateCompilerSettings() {
        List<String> inputArguments = runtimeBean.getInputArguments();
        
        boolean isServerMode = inputArguments.stream().anyMatch(arg -> arg.contains("-server"));
        if (isServerMode) {
            log.info("✓ Running in server mode (recommended for production)");
        } else {
            log.warn("Not explicitly running in server mode - add -server flag");
        }
        
        // Check for tiered compilation
        boolean hasTieredCompilation = inputArguments.stream().anyMatch(arg -> arg.contains("TieredCompilation"));
        if (hasTieredCompilation) {
            log.info("✓ Tiered compilation setting found");
        } else {
            log.info("Consider enabling tiered compilation for better startup performance");
        }
    }
    
    /**
     * Configure runtime optimizations
     */
    private void configureOptimizations() {
        log.info("=== Configuring Runtime Optimizations ===");
        
        // Set system properties for optimal performance
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.awt.headless", "true");
        
        // Configure NIO optimizations
        System.setProperty("sun.nio.ch.bugLevel", "");
        System.setProperty("sun.nio.useCanonicalPrefixCache", "false");
        
        // Configure networking optimizations
        System.setProperty("networkaddress.cache.ttl", "60");
        System.setProperty("networkaddress.cache.negative.ttl", "10");
        
        // Configure security optimizations
        System.setProperty("java.security.egd", "file:/dev/./urandom");
        
        log.info("Runtime optimizations configured");
    }
    
    /**
     * Start performance monitoring
     */
    private void startPerformanceMonitoring() {
        log.info("Starting JVM performance monitoring...");
        
        // Schedule periodic monitoring
        java.util.concurrent.Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                this::logPerformanceMetrics,
                60, // Initial delay
                300, // Period (5 minutes)
                java.util.concurrent.TimeUnit.SECONDS
        );
    }
    
    /**
     * Log performance metrics
     */
    private void logPerformanceMetrics() {
        try {
            // Memory metrics
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            
            // GC metrics
            long gcCount = getGCCount();
            long gcTime = getGCTime();
            
            // Thread metrics
            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
            int peakThreadCount = ManagementFactory.getThreadMXBean().getPeakThreadCount();
            
            log.info("=== JVM Performance Metrics ===");
            log.info("Memory Usage: {:.1f}% ({} MB / {} MB)", 
                    memoryUsage, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            log.info("GC Count: {}, GC Time: {} ms", gcCount, gcTime);
            log.info("Thread Count: {} (Peak: {})", threadCount, peakThreadCount);
            log.info("Uptime: {} minutes", runtimeBean.getUptime() / 60000);
            
            // Performance warnings
            if (memoryUsage > 80) {
                log.warn("High memory usage detected: {:.1f}%", memoryUsage);
            }
            
            if (gcTime > 1000) {
                log.warn("High GC time detected: {} ms", gcTime);
            }
            
        } catch (Exception e) {
            log.error("Error collecting performance metrics", e);
        }
    }
    
    /**
     * Get total GC count
     */
    private long getGCCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gcBean -> gcBean.getCollectionCount())
                .sum();
    }
    
    /**
     * Get total GC time
     */
    private long getGCTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gcBean -> gcBean.getCollectionTime())
                .sum();
    }
    
    /**
     * Generate JVM tuning recommendations
     */
    public java.util.List<String> getJVMTuningRecommendations() {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        List<String> inputArguments = runtimeBean.getInputArguments();
        
        // Memory recommendations
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        
        if (maxMemory != totalMemory) {
            recommendations.add("Set -Xms equal to -Xmx to avoid dynamic heap expansion");
        }
        
        // GC recommendations
        boolean hasG1GC = inputArguments.stream().anyMatch(arg -> arg.contains("UseG1GC"));
        if (!hasG1GC) {
            recommendations.add("Consider using G1GC: -XX:+UseG1GC -XX:MaxGCPauseMillis=10");
        }
        
        // Compiler recommendations
        boolean isServerMode = inputArguments.stream().anyMatch(arg -> arg.contains("-server"));
        if (!isServerMode) {
            recommendations.add("Add -server flag for production deployment");
        }
        
        // Performance recommendations
        recommendations.add("Consider adding: -XX:+UseStringDeduplication");
        recommendations.add("Consider adding: -XX:+OptimizeStringConcat");
        recommendations.add("Consider adding: -XX:+UseFastAccessorMethods");
        
        // NUMA recommendations
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors > 8) {
            recommendations.add("Consider NUMA optimizations: -XX:+UseNUMA");
        }
        
        return recommendations;
    }
    
    /**
     * Get current JVM performance summary
     */
    public JVMPerformanceSummary getPerformanceSummary() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) usedMemory / maxMemory * 100;
        
        long gcCount = getGCCount();
        long gcTime = getGCTime();
        
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        long uptime = runtimeBean.getUptime();
        
        return new JVMPerformanceSummary(
                memoryUsage,
                usedMemory / 1024 / 1024,
                maxMemory / 1024 / 1024,
                gcCount,
                gcTime,
                threadCount,
                uptime
        );
    }
    
    /**
     * JVM performance summary
     */
    public static class JVMPerformanceSummary {
        private final double memoryUsagePercent;
        private final long usedMemoryMB;
        private final long maxMemoryMB;
        private final long gcCount;
        private final long gcTimeMs;
        private final int threadCount;
        private final long uptimeMs;
        
        public JVMPerformanceSummary(double memoryUsagePercent, long usedMemoryMB, long maxMemoryMB,
                                   long gcCount, long gcTimeMs, int threadCount, long uptimeMs) {
            this.memoryUsagePercent = memoryUsagePercent;
            this.usedMemoryMB = usedMemoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.gcCount = gcCount;
            this.gcTimeMs = gcTimeMs;
            this.threadCount = threadCount;
            this.uptimeMs = uptimeMs;
        }
        
        // Getters
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public long getUsedMemoryMB() { return usedMemoryMB; }
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public long getGcCount() { return gcCount; }
        public long getGcTimeMs() { return gcTimeMs; }
        public int getThreadCount() { return threadCount; }
        public long getUptimeMs() { return uptimeMs; }
        
        @Override
        public String toString() {
            return String.format("JVMPerformanceSummary{memory=%.1f%% (%dMB/%dMB), gc=%d/%dms, threads=%d, uptime=%dmin}",
                    memoryUsagePercent, usedMemoryMB, maxMemoryMB, gcCount, gcTimeMs, threadCount, uptimeMs / 60000);
        }
    }
}