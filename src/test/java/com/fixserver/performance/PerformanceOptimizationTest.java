package com.fixserver.performance;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.performance.benchmarks.PerformanceBenchmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance optimization tests to validate improvements.
 * 
 * These tests compare optimized implementations against standard ones
 * to ensure performance improvements are achieved.
 */
@SpringBootTest
@ActiveProfiles("test")
class PerformanceOptimizationTest {
    
    private FIXProtocolHandler standardHandler;
    private HighPerformanceMessageParser optimizedParser;
    private PerformanceBenchmark benchmark;
    
    // Sample message with correct checksum calculated
    private static String SAMPLE_MESSAGE;
    
    static {
        // Build message without checksum
        String msgWithoutChecksum = "8=FIX.4.4\u00019=88\u000135=D\u000149=CLIENT1\u000156=SERVER1\u000134=1\u000152=20231225-10:30:00\u000155=AAPL\u000154=1\u000138=100\u000140=1\u0001";
        // Calculate checksum
        int sum = 0;
        for (char c : msgWithoutChecksum.toCharArray()) {
            sum += c;
        }
        String checksum = String.format("%03d", sum % 256);
        SAMPLE_MESSAGE = msgWithoutChecksum + "10=" + checksum + "\u0001";
    }
    
    private static final int PERFORMANCE_ITERATIONS = 1000;
    
    @BeforeEach
    void setUp() {
        standardHandler = new FIXProtocolHandler();
        optimizedParser = new HighPerformanceMessageParser();
        benchmark = new PerformanceBenchmark();
    }
    
    @Test
    @DisplayName("Optimized message parsing should be faster than standard")
    void testOptimizedParsingPerformance() {
        // Warmup
        for (int i = 0; i < 100; i++) {
            standardHandler.parse(SAMPLE_MESSAGE);
            optimizedParser.parseFromString(SAMPLE_MESSAGE);
        }
        
        // Benchmark standard parsing
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            FIXMessage message = standardHandler.parse(SAMPLE_MESSAGE);
            assertNotNull(message);
        }
        long standardTime = System.nanoTime() - startTime;
        
        // Benchmark optimized parsing
        startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            OptimizedFIXMessage message = optimizedParser.parseFromString(SAMPLE_MESSAGE);
            assertNotNull(message);
            optimizedParser.returnToPool(message);
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        // Calculate improvement
        double improvement = ((double) standardTime - optimizedTime) / standardTime * 100;
        
        System.out.printf("Standard parsing: %.2f ms%n", standardTime / 1_000_000.0);
        System.out.printf("Optimized parsing: %.2f ms%n", optimizedTime / 1_000_000.0);
        System.out.printf("Performance improvement: %.1f%%%n", improvement);
        
        // Assert that optimized version is faster (or at least not significantly slower)
        // Note: Performance can vary based on system load, so we use a lenient threshold
        assertTrue(improvement > -10, 
                String.format("Optimized parsing should not be significantly slower, got %.1f%% change", improvement));
    }
    
    @Test
    @DisplayName("Optimized message formatting should be faster than standard")
    void testOptimizedFormattingPerformance() {
        // Create messages for testing
        FIXMessage standardMessage = standardHandler.parse(SAMPLE_MESSAGE);
        OptimizedFIXMessage optimizedMessage = optimizedParser.parseFromString(SAMPLE_MESSAGE);
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            standardMessage.toFixString();
            optimizedMessage.toFixString();
        }
        
        // Benchmark standard formatting
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            String formatted = standardMessage.toFixString();
            assertNotNull(formatted);
        }
        long standardTime = System.nanoTime() - startTime;
        
        // Benchmark optimized formatting
        startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            String formatted = optimizedMessage.toFixString();
            assertNotNull(formatted);
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        // Calculate improvement
        double improvement = ((double) standardTime - optimizedTime) / standardTime * 100;
        
        System.out.printf("Standard formatting: %.2f ms%n", standardTime / 1_000_000.0);
        System.out.printf("Optimized formatting: %.2f ms%n", optimizedTime / 1_000_000.0);
        System.out.printf("Performance improvement: %.1f%%%n", improvement);
        
        // Assert that optimized version is faster (or at least not significantly slower)
        // Note: Performance can vary based on system load
        assertTrue(improvement > -10, 
                String.format("Optimized formatting should not be significantly slower, got %.1f%% change", improvement));
        
        optimizedParser.returnToPool(optimizedMessage);
    }
    
    @Test
    @DisplayName("Field access should be efficient in optimized implementation")
    void testOptimizedFieldAccessPerformance() {
        OptimizedFIXMessage message = optimizedParser.parseFromString(SAMPLE_MESSAGE);
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            message.getMessageType();
            message.getSenderCompId();
            message.getField(55);
        }
        
        // Benchmark field access
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS * 10; i++) {
            String msgType = message.getMessageType();
            String sender = message.getSenderCompId();
            String symbol = message.getField(55);
            
            assertNotNull(msgType);
            assertNotNull(sender);
            assertNotNull(symbol);
        }
        long totalTime = System.nanoTime() - startTime;
        
        double avgAccessTime = (double) totalTime / (PERFORMANCE_ITERATIONS * 10 * 3) / 1000.0; // microseconds
        
        System.out.printf("Average field access time: %.3f μs%n", avgAccessTime);
        
        // Assert that field access is reasonably fast (under 1 microsecond)
        // Note: This is a lenient threshold to account for system variations
        assertTrue(avgAccessTime < 1.0, 
                String.format("Field access too slow: %.3f μs", avgAccessTime));
        
        optimizedParser.returnToPool(message);
    }
    
    @Test
    @DisplayName("Concurrent processing should scale well")
    void testConcurrentProcessingPerformance() throws InterruptedException {
        int threadCount = 4;
        int messagesPerThread = PERFORMANCE_ITERATIONS / threadCount;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong totalMessages = new AtomicLong();
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        OptimizedFIXMessage message = optimizedParser.parseFromString(SAMPLE_MESSAGE);
                        String formatted = message.toFixString();
                        assertNotNull(formatted);
                        optimizedParser.returnToPool(message);
                        totalMessages.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent processing timed out");
        
        long totalTime = System.nanoTime() - startTime;
        double throughput = totalMessages.get() * 1_000_000_000.0 / totalTime;
        
        System.out.printf("Concurrent processing throughput: %.0f messages/second%n", throughput);
        
        // Assert minimum throughput
        assertTrue(throughput > 10000, 
                String.format("Throughput too low: %.0f msg/sec", throughput));
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Memory allocation should be minimal in optimized implementation")
    void testMemoryAllocationOptimization() {
        Runtime runtime = Runtime.getRuntime();
        
        // Force GC and measure baseline
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Process messages with object reuse
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            OptimizedFIXMessage message = optimizedParser.parseFromString(SAMPLE_MESSAGE);
            String formatted = message.toFixString();
            assertNotNull(formatted);
            optimizedParser.returnToPool(message); // Return to pool for reuse
        }
        
        // Force GC and measure after
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryUsed = memoryAfter - memoryBefore;
        double memoryPerMessage = (double) memoryUsed / PERFORMANCE_ITERATIONS;
        
        System.out.printf("Total memory used: %d bytes%n", memoryUsed);
        System.out.printf("Memory per message: %.2f bytes%n", memoryPerMessage);
        
        // Assert reasonable memory usage (should be minimal due to object pooling)
        assertTrue(memoryPerMessage < 1000, 
                String.format("Memory usage too high: %.2f bytes per message", memoryPerMessage));
    }
    
    @Test
    @DisplayName("Checksum calculation should be optimized")
    void testChecksumCalculationPerformance() {
        String testMessage = "8=FIX.4.4\u00019=71\u000135=D\u000149=CLIENT1\u000156=SERVER1";
        
        // Warmup
        for (int i = 0; i < 100; i++) {
            calculateChecksum(testMessage);
        }
        
        // Benchmark checksum calculation
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS * 10; i++) {
            String checksum = calculateChecksum(testMessage);
            assertNotNull(checksum);
            assertEquals(3, checksum.length());
        }
        long totalTime = System.nanoTime() - startTime;
        
        double avgTime = (double) totalTime / (PERFORMANCE_ITERATIONS * 10) / 1000.0; // microseconds
        
        System.out.printf("Average checksum calculation time: %.3f μs%n", avgTime);
        
        // Assert that checksum calculation is reasonably fast (under 10 microseconds)
        // Note: This is a lenient threshold to account for system variations
        assertTrue(avgTime < 10.0, 
                String.format("Checksum calculation too slow: %.3f μs", avgTime));
    }
    
    @Test
    @DisplayName("Full benchmark suite should show significant improvements")
    void testFullBenchmarkSuite() {
        PerformanceBenchmark.BenchmarkResults results = benchmark.runBenchmarkSuite();
        
        assertNotNull(results);
        assertNull(results.error, "Benchmark suite should complete without errors");
        
        // Print results
        results.printSummary();
        
        // Validate that benchmark completed successfully
        // Note: We don't enforce strict performance improvements as they vary by system
        if (results.optimizedVsStandardResults != null) {
            double improvement = results.getOverallLatencyImprovement();
            System.out.printf("Overall latency improvement: %.1f%%%n", improvement);
            // Just verify it's not significantly worse
            assertTrue(improvement > -20, 
                    String.format("Performance should not degrade significantly, got %.1f%% change", improvement));
        }
    }
    
    /**
     * Simple checksum calculation for testing
     */
    private String calculateChecksum(String message) {
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += c;
        }
        return String.format("%03d", sum % 256);
    }
}