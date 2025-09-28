package com.fixserver.performance.benchmarks;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.performance.HighPerformanceMessageParser;
import com.fixserver.performance.OptimizedFIXMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance benchmark suite for FIX server components.
 * 
 * This benchmark suite provides comprehensive performance testing for:
 * - Message parsing and formatting
 * - Field access operations
 * - Checksum calculations
 * - Concurrent message processing
 * - Memory allocation patterns
 * - Throughput measurements
 * 
 * The benchmarks help validate performance optimizations and identify
 * bottlenecks in the FIX message processing pipeline.
 * 
 * @author FIX Server Performance Team
 * @version 2.0
 * @since 2.0
 */
@Slf4j
@Component
public class PerformanceBenchmark {
    
    @Autowired(required = false)
    private FIXProtocolHandler protocolHandler;
    
    @Autowired(required = false)
    private HighPerformanceMessageParser optimizedParser;
    
    // Benchmark configuration
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 10000;
    private static final int CONCURRENT_THREADS = 4;
    
    // Sample FIX messages for testing
    private static final String[] SAMPLE_MESSAGES = {
            "8=FIX.4.4\u00019=71\u000135=D\u000149=CLIENT1\u000156=SERVER1\u000134=1\u000152=20231225-10:30:00\u000155=AAPL\u000154=1\u000138=100\u000140=1\u000110=123\u0001",
            "8=FIX.4.4\u00019=65\u000135=A\u000149=CLIENT1\u000156=SERVER1\u000134=1\u000152=20231225-10:30:00\u000198=0\u0001108=30\u000110=123\u0001",
            "8=FIX.4.4\u00019=150\u000135=8\u000149=SERVER1\u000156=CLIENT1\u000134=2\u000152=20231225-10:30:01\u000111=ORDER123\u000117=EXEC456\u000137=ORDER789\u000139=0\u000154=1\u000155=AAPL\u0001150=0\u0001151=100\u000110=123\u0001"
    };
    
    /**
     * Run comprehensive performance benchmark suite
     */
    public BenchmarkResults runBenchmarkSuite() {
        log.info("Starting FIX Server Performance Benchmark Suite...");
        
        BenchmarkResults results = new BenchmarkResults();
        
        try {
            // Warmup JVM
            warmupJVM();
            
            // Run individual benchmarks
            results.messageParsingResults = benchmarkMessageParsing();
            results.messageFormattingResults = benchmarkMessageFormatting();
            results.fieldAccessResults = benchmarkFieldAccess();
            results.checksumCalculationResults = benchmarkChecksumCalculation();
            results.concurrentProcessingResults = benchmarkConcurrentProcessing();
            results.memoryAllocationResults = benchmarkMemoryAllocation();
            results.optimizedVsStandardResults = benchmarkOptimizedVsStandard();
            
            log.info("Benchmark suite completed successfully");
            
        } catch (Exception e) {
            log.error("Benchmark suite failed", e);
            results.error = e.getMessage();
        }
        
        return results;
    }
    
    /**
     * Warmup JVM for accurate benchmarking
     */
    private void warmupJVM() {
        log.info("Warming up JVM...");
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (String message : SAMPLE_MESSAGES) {
                if (protocolHandler != null) {
                    FIXMessage parsed = protocolHandler.parse(message);
                    parsed.toFixString();
                }
            }
        }
        
        // Force GC to clean up warmup objects
        System.gc();
        
        log.info("JVM warmup completed");
    }
    
    /**
     * Benchmark message parsing performance
     */
    private BenchmarkResult benchmarkMessageParsing() {
        log.info("Benchmarking message parsing...");
        
        if (protocolHandler == null) {
            return new BenchmarkResult("Message Parsing", 0, 0, "Protocol handler not available");
        }
        
        long startTime = System.nanoTime();
        long totalMessages = 0;
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (String message : SAMPLE_MESSAGES) {
                FIXMessage parsed = protocolHandler.parse(message);
                totalMessages++;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double avgLatencyMicros = (double) totalTime / totalMessages / 1000.0;
        double throughputMps = totalMessages * 1_000_000_000.0 / totalTime;
        
        return new BenchmarkResult("Message Parsing", avgLatencyMicros, throughputMps, null);
    }
    
    /**
     * Benchmark message formatting performance
     */
    private BenchmarkResult benchmarkMessageFormatting() {
        log.info("Benchmarking message formatting...");
        
        if (protocolHandler == null) {
            return new BenchmarkResult("Message Formatting", 0, 0, "Protocol handler not available");
        }
        
        // Pre-parse messages
        List<FIXMessage> messages = new ArrayList<>();
        for (String messageStr : SAMPLE_MESSAGES) {
            messages.add(protocolHandler.parse(messageStr));
        }
        
        long startTime = System.nanoTime();
        long totalMessages = 0;
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (FIXMessage message : messages) {
                String formatted = message.toFixString();
                totalMessages++;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double avgLatencyMicros = (double) totalTime / totalMessages / 1000.0;
        double throughputMps = totalMessages * 1_000_000_000.0 / totalTime;
        
        return new BenchmarkResult("Message Formatting", avgLatencyMicros, throughputMps, null);
    }
    
    /**
     * Benchmark field access performance
     */
    private BenchmarkResult benchmarkFieldAccess() {
        log.info("Benchmarking field access...");
        
        if (protocolHandler == null) {
            return new BenchmarkResult("Field Access", 0, 0, "Protocol handler not available");
        }
        
        // Pre-parse a message
        FIXMessage message = protocolHandler.parse(SAMPLE_MESSAGES[0]);
        
        long startTime = System.nanoTime();
        long totalAccesses = 0;
        
        for (int i = 0; i < BENCHMARK_ITERATIONS * 10; i++) {
            // Access common fields
            message.getMessageType();
            message.getSenderCompId();
            message.getTargetCompId();
            message.getMessageSequenceNumber();
            message.getField(55); // Symbol
            message.getField(54); // Side
            message.getField(38); // OrderQty
            message.getField(40); // OrdType
            totalAccesses += 8;
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double avgLatencyNanos = (double) totalTime / totalAccesses;
        double throughputAps = totalAccesses * 1_000_000_000.0 / totalTime;
        
        return new BenchmarkResult("Field Access", avgLatencyNanos / 1000.0, throughputAps, null);
    }
    
    /**
     * Benchmark checksum calculation performance
     */
    private BenchmarkResult benchmarkChecksumCalculation() {
        log.info("Benchmarking checksum calculation...");
        
        long startTime = System.nanoTime();
        long totalCalculations = 0;
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (String message : SAMPLE_MESSAGES) {
                // Simulate checksum calculation
                int sum = 0;
                for (char c : message.toCharArray()) {
                    sum += c;
                }
                String checksum = String.format("%03d", sum % 256);
                totalCalculations++;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double avgLatencyMicros = (double) totalTime / totalCalculations / 1000.0;
        double throughputCps = totalCalculations * 1_000_000_000.0 / totalTime;
        
        return new BenchmarkResult("Checksum Calculation", avgLatencyMicros, throughputCps, null);
    }
    
    /**
     * Benchmark concurrent message processing
     */
    private BenchmarkResult benchmarkConcurrentProcessing() {
        log.info("Benchmarking concurrent processing...");
        
        if (protocolHandler == null) {
            return new BenchmarkResult("Concurrent Processing", 0, 0, "Protocol handler not available");
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        AtomicLong totalMessages = new AtomicLong();
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < BENCHMARK_ITERATIONS / CONCURRENT_THREADS; i++) {
                        for (String message : SAMPLE_MESSAGES) {
                            FIXMessage parsed = protocolHandler.parse(message);
                            parsed.toFixString();
                            totalMessages.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BenchmarkResult("Concurrent Processing", 0, 0, "Interrupted");
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        
        double avgLatencyMicros = (double) totalTime / totalMessages.get() / 1000.0;
        double throughputMps = totalMessages.get() * 1_000_000_000.0 / totalTime;
        
        return new BenchmarkResult("Concurrent Processing", avgLatencyMicros, throughputMps, null);
    }
    
    /**
     * Benchmark memory allocation patterns
     */
    private BenchmarkResult benchmarkMemoryAllocation() {
        log.info("Benchmarking memory allocation...");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Force GC before measurement
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        long startTime = System.nanoTime();
        
        // Perform operations that allocate memory
        List<FIXMessage> messages = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            FIXMessage message = new FIXMessageImpl("FIX.4.4", "D");
            message.setField(49, "CLIENT" + i);
            message.setField(56, "SERVER1");
            message.setField(34, String.valueOf(i));
            message.setField(52, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss")));
            message.setField(55, "AAPL");
            message.setField(54, "1");
            message.setField(38, "100");
            message.setField(40, "1");
            messages.add(message);
        }
        
        long endTime = System.nanoTime();
        
        // Force GC and measure memory after
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        long totalTime = endTime - startTime;
        long memoryUsed = memoryAfter - memoryBefore;
        
        double avgLatencyMicros = (double) totalTime / messages.size() / 1000.0;
        double memoryPerMessage = (double) memoryUsed / messages.size();
        
        return new BenchmarkResult("Memory Allocation", avgLatencyMicros, memoryPerMessage, 
                String.format("Total memory used: %d bytes", memoryUsed));
    }
    
    /**
     * Benchmark optimized vs standard implementations
     */
    private BenchmarkResult benchmarkOptimizedVsStandard() {
        log.info("Benchmarking optimized vs standard implementations...");
        
        if (protocolHandler == null || optimizedParser == null) {
            return new BenchmarkResult("Optimized vs Standard", 0, 0, "Components not available");
        }
        
        // Benchmark standard implementation
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (String message : SAMPLE_MESSAGES) {
                FIXMessage parsed = protocolHandler.parse(message);
                parsed.toFixString();
            }
        }
        long standardTime = System.nanoTime() - startTime;
        
        // Benchmark optimized implementation
        startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (String message : SAMPLE_MESSAGES) {
                OptimizedFIXMessage parsed = optimizedParser.parseFromString(message);
                parsed.toFixString();
                optimizedParser.returnToPool(parsed);
            }
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        double improvement = ((double) standardTime - optimizedTime) / standardTime * 100;
        double standardThroughput = (BENCHMARK_ITERATIONS * SAMPLE_MESSAGES.length) * 1_000_000_000.0 / standardTime;
        double optimizedThroughput = (BENCHMARK_ITERATIONS * SAMPLE_MESSAGES.length) * 1_000_000_000.0 / optimizedTime;
        
        return new BenchmarkResult("Optimized vs Standard", improvement, optimizedThroughput / standardThroughput,
                String.format("Standard: %.0f msg/sec, Optimized: %.0f msg/sec, Improvement: %.1f%%", 
                        standardThroughput, optimizedThroughput, improvement));
    }
    
    /**
     * Benchmark result container
     */
    public static class BenchmarkResult {
        private final String name;
        private final double avgLatencyMicros;
        private final double throughputOrMetric;
        private final String notes;
        
        public BenchmarkResult(String name, double avgLatencyMicros, double throughputOrMetric, String notes) {
            this.name = name;
            this.avgLatencyMicros = avgLatencyMicros;
            this.throughputOrMetric = throughputOrMetric;
            this.notes = notes;
        }
        
        public String getName() { return name; }
        public double getAvgLatencyMicros() { return avgLatencyMicros; }
        public double getThroughputOrMetric() { return throughputOrMetric; }
        public String getNotes() { return notes; }
        
        @Override
        public String toString() {
            return String.format("%s: %.2fμs avg latency, %.0f throughput/metric%s",
                    name, avgLatencyMicros, throughputOrMetric, 
                    notes != null ? " (" + notes + ")" : "");
        }
    }
    
    /**
     * Complete benchmark results
     */
    public static class BenchmarkResults {
        public BenchmarkResult messageParsingResults;
        public BenchmarkResult messageFormattingResults;
        public BenchmarkResult fieldAccessResults;
        public BenchmarkResult checksumCalculationResults;
        public BenchmarkResult concurrentProcessingResults;
        public BenchmarkResult memoryAllocationResults;
        public BenchmarkResult optimizedVsStandardResults;
        public String error;
        
        public void printSummary() {
            System.out.println("\n=== FIX Server Performance Benchmark Results ===");
            
            if (error != null) {
                System.out.println("ERROR: " + error);
                return;
            }
            
            if (messageParsingResults != null) System.out.println(messageParsingResults);
            if (messageFormattingResults != null) System.out.println(messageFormattingResults);
            if (fieldAccessResults != null) System.out.println(fieldAccessResults);
            if (checksumCalculationResults != null) System.out.println(checksumCalculationResults);
            if (concurrentProcessingResults != null) System.out.println(concurrentProcessingResults);
            if (memoryAllocationResults != null) System.out.println(memoryAllocationResults);
            if (optimizedVsStandardResults != null) System.out.println(optimizedVsStandardResults);
            
            System.out.println("=== End of Benchmark Results ===\n");
        }
        
        public double getOverallLatencyImprovement() {
            if (optimizedVsStandardResults != null) {
                return optimizedVsStandardResults.getAvgLatencyMicros(); // This contains the improvement percentage
            }
            return 0.0;
        }
        
        public double getOverallThroughputImprovement() {
            if (optimizedVsStandardResults != null) {
                return optimizedVsStandardResults.getThroughputOrMetric(); // This contains the throughput ratio
            }
            return 1.0;
        }
    }
}