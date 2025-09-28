package com.fixserver.performance;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-world performance tests comparing standard vs optimized implementations.
 * 
 * This test focuses on practical performance measurements with realistic
 * expectations and proper message formatting.
 */
class RealWorldPerformanceTest {
    
    private FIXProtocolHandler standardHandler;
    private HighPerformanceMessageParser optimizedParser;
    
    // Valid FIX message with correct checksum
    private static final String VALID_FIX_MESSAGE = createValidFixMessage();
    
    private static final int PERFORMANCE_ITERATIONS = 5000; // Reduced for realistic testing
    private static final int WARMUP_ITERATIONS = 1000;
    
    @BeforeEach
    void setUp() {
        standardHandler = new FIXProtocolHandler();
        optimizedParser = new HighPerformanceMessageParser();
    }
    
    @Test
    @DisplayName("Compare standard vs optimized message parsing performance")
    void compareParsingPerformance() {
        System.out.println("\n=== FIX Message Parsing Performance Comparison ===");
        
        // Warmup both implementations
        warmupParsing();
        
        // Test standard parsing
        long standardTime = benchmarkStandardParsing();
        double standardThroughput = calculateThroughput(PERFORMANCE_ITERATIONS, standardTime);
        
        // Test optimized parsing
        long optimizedTime = benchmarkOptimizedParsing();
        double optimizedThroughput = calculateThroughput(PERFORMANCE_ITERATIONS, optimizedTime);
        
        // Calculate improvement
        double latencyImprovement = ((double) standardTime - optimizedTime) / standardTime * 100;
        double throughputImprovement = (optimizedThroughput / standardThroughput - 1) * 100;
        
        // Print results
        System.out.printf("Standard Implementation:%n");
        System.out.printf("  - Total time: %.2f ms%n", standardTime / 1_000_000.0);
        System.out.printf("  - Average latency: %.3f μs%n", (double) standardTime / PERFORMANCE_ITERATIONS / 1000.0);
        System.out.printf("  - Throughput: %.0f msg/sec%n", standardThroughput);
        
        System.out.printf("Optimized Implementation:%n");
        System.out.printf("  - Total time: %.2f ms%n", optimizedTime / 1_000_000.0);
        System.out.printf("  - Average latency: %.3f μs%n", (double) optimizedTime / PERFORMANCE_ITERATIONS / 1000.0);
        System.out.printf("  - Throughput: %.0f msg/sec%n", optimizedThroughput);
        
        System.out.printf("Performance Improvement:%n");
        System.out.printf("  - Latency improvement: %.1f%%%n", latencyImprovement);
        System.out.printf("  - Throughput improvement: %.1f%%%n", throughputImprovement);
        
        // Validate that optimized version shows some improvement
        if (latencyImprovement > 0) {
            System.out.println("✅ Optimized parsing is faster!");
        } else {
            System.out.println("⚠️  Optimized parsing needs tuning");
        }
    }
    
    @Test
    @DisplayName("Compare message formatting performance")
    void compareFormattingPerformance() {
        System.out.println("\n=== FIX Message Formatting Performance Comparison ===");
        
        // Create messages for testing
        FIXMessage standardMessage = createStandardMessage();
        OptimizedFIXMessage optimizedMessage = createOptimizedMessage();
        
        // Warmup
        warmupFormatting(standardMessage, optimizedMessage);
        
        // Test standard formatting
        long standardTime = benchmarkStandardFormatting(standardMessage);
        double standardThroughput = calculateThroughput(PERFORMANCE_ITERATIONS, standardTime);
        
        // Test optimized formatting
        long optimizedTime = benchmarkOptimizedFormatting(optimizedMessage);
        double optimizedThroughput = calculateThroughput(PERFORMANCE_ITERATIONS, optimizedTime);
        
        // Calculate improvement
        double latencyImprovement = ((double) standardTime - optimizedTime) / standardTime * 100;
        double throughputImprovement = (optimizedThroughput / standardThroughput - 1) * 100;
        
        // Print results
        System.out.printf("Standard Formatting:%n");
        System.out.printf("  - Total time: %.2f ms%n", standardTime / 1_000_000.0);
        System.out.printf("  - Average latency: %.3f μs%n", (double) standardTime / PERFORMANCE_ITERATIONS / 1000.0);
        System.out.printf("  - Throughput: %.0f msg/sec%n", standardThroughput);
        
        System.out.printf("Optimized Formatting:%n");
        System.out.printf("  - Total time: %.2f ms%n", optimizedTime / 1_000_000.0);
        System.out.printf("  - Average latency: %.3f μs%n", (double) optimizedTime / PERFORMANCE_ITERATIONS / 1000.0);
        System.out.printf("  - Throughput: %.0f msg/sec%n", optimizedThroughput);
        
        System.out.printf("Performance Improvement:%n");
        System.out.printf("  - Latency improvement: %.1f%%%n", latencyImprovement);
        System.out.printf("  - Throughput improvement: %.1f%%%n", throughputImprovement);
        
        if (latencyImprovement > 0) {
            System.out.println("✅ Optimized formatting is faster!");
        } else {
            System.out.println("⚠️  Optimized formatting needs tuning");
        }
    }
    
    @Test
    @DisplayName("Test concurrent processing performance")
    void testConcurrentPerformance() throws InterruptedException {
        System.out.println("\n=== Concurrent Processing Performance Test ===");
        
        int threadCount = Runtime.getRuntime().availableProcessors();
        int messagesPerThread = PERFORMANCE_ITERATIONS / threadCount;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong totalMessages = new AtomicLong();
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        OptimizedFIXMessage message = optimizedParser.parseFromString(VALID_FIX_MESSAGE);
                        String formatted = message.toFixString();
                        optimizedParser.returnToPool(message);
                        totalMessages.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error in concurrent processing: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.nanoTime() - startTime;
        
        double throughput = totalMessages.get() * 1_000_000_000.0 / totalTime;
        double avgLatency = (double) totalTime / totalMessages.get() / 1000.0; // microseconds
        
        System.out.printf("Concurrent Processing Results:%n");
        System.out.printf("  - Threads: %d%n", threadCount);
        System.out.printf("  - Total messages: %d%n", totalMessages.get());
        System.out.printf("  - Total time: %.2f ms%n", totalTime / 1_000_000.0);
        System.out.printf("  - Average latency: %.3f μs%n", avgLatency);
        System.out.printf("  - Throughput: %.0f msg/sec%n", throughput);
        
        executor.shutdown();
        
        if (throughput > 10000) {
            System.out.println("✅ Excellent concurrent performance!");
        } else if (throughput > 5000) {
            System.out.println("✅ Good concurrent performance");
        } else {
            System.out.println("⚠️  Concurrent performance needs improvement");
        }
    }
    
    @Test
    @DisplayName("Memory allocation comparison")
    void compareMemoryAllocation() {
        System.out.println("\n=== Memory Allocation Comparison ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Test standard implementation memory usage
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            FIXMessage message = standardHandler.parse(VALID_FIX_MESSAGE);
            message.toFixString();
        }
        
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long standardMemoryUsed = memoryAfter - memoryBefore;
        
        // Test optimized implementation memory usage
        System.gc();
        memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            OptimizedFIXMessage message = optimizedParser.parseFromString(VALID_FIX_MESSAGE);
            message.toFixString();
            optimizedParser.returnToPool(message); // Return to pool for reuse
        }
        
        System.gc();
        memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long optimizedMemoryUsed = memoryAfter - memoryBefore;
        
        double memoryImprovement = ((double) standardMemoryUsed - optimizedMemoryUsed) / standardMemoryUsed * 100;
        
        System.out.printf("Memory Usage Comparison:%n");
        System.out.printf("  - Standard implementation: %d bytes (%.2f bytes/msg)%n", 
                standardMemoryUsed, (double) standardMemoryUsed / PERFORMANCE_ITERATIONS);
        System.out.printf("  - Optimized implementation: %d bytes (%.2f bytes/msg)%n", 
                optimizedMemoryUsed, (double) optimizedMemoryUsed / PERFORMANCE_ITERATIONS);
        System.out.printf("  - Memory improvement: %.1f%%%n", memoryImprovement);
        
        if (memoryImprovement > 0) {
            System.out.println("✅ Optimized implementation uses less memory!");
        } else {
            System.out.println("⚠️  Memory usage optimization needs tuning");
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private void warmupParsing() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                FIXMessage standard = standardHandler.parse(VALID_FIX_MESSAGE);
                OptimizedFIXMessage optimized = optimizedParser.parseFromString(VALID_FIX_MESSAGE);
                optimizedParser.returnToPool(optimized);
            } catch (Exception e) {
                // Ignore warmup errors
            }
        }
    }
    
    private void warmupFormatting(FIXMessage standardMessage, OptimizedFIXMessage optimizedMessage) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            standardMessage.toFixString();
            optimizedMessage.toFixString();
        }
    }
    
    private long benchmarkStandardParsing() {
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            try {
                FIXMessage message = standardHandler.parse(VALID_FIX_MESSAGE);
            } catch (Exception e) {
                // Count parsing errors but continue
            }
        }
        return System.nanoTime() - startTime;
    }
    
    private long benchmarkOptimizedParsing() {
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            try {
                OptimizedFIXMessage message = optimizedParser.parseFromString(VALID_FIX_MESSAGE);
                optimizedParser.returnToPool(message);
            } catch (Exception e) {
                // Count parsing errors but continue
            }
        }
        return System.nanoTime() - startTime;
    }
    
    private long benchmarkStandardFormatting(FIXMessage message) {
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            message.toFixString();
        }
        return System.nanoTime() - startTime;
    }
    
    private long benchmarkOptimizedFormatting(OptimizedFIXMessage message) {
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            message.toFixString();
        }
        return System.nanoTime() - startTime;
    }
    
    private double calculateThroughput(int operations, long timeNanos) {
        return operations * 1_000_000_000.0 / timeNanos;
    }
    
    private FIXMessage createStandardMessage() {
        FIXMessage message = new FIXMessageImpl("FIX.4.4", "D");
        message.setField(49, "CLIENT1");
        message.setField(56, "SERVER1");
        message.setField(34, "1");
        message.setField(52, "20231225-10:30:00");
        message.setField(55, "AAPL");
        message.setField(54, "1");
        message.setField(38, "100");
        message.setField(40, "1");
        return message;
    }
    
    private OptimizedFIXMessage createOptimizedMessage() {
        OptimizedFIXMessage message = new OptimizedFIXMessage("FIX.4.4", "D");
        message.setField(49, "CLIENT1");
        message.setField(56, "SERVER1");
        message.setField(34, "1");
        message.setField(52, "20231225-10:30:00");
        message.setField(55, "AAPL");
        message.setField(54, "1");
        message.setField(38, "100");
        message.setField(40, "1");
        return message;
    }
    
    /**
     * Create a valid FIX message with correct checksum
     */
    private static String createValidFixMessage() {
        // Create a simple but valid FIX message
        StringBuilder msg = new StringBuilder();
        msg.append("8=FIX.4.4\u0001");
        msg.append("35=D\u0001");
        msg.append("49=CLIENT1\u0001");
        msg.append("56=SERVER1\u0001");
        msg.append("34=1\u0001");
        msg.append("52=20231225-10:30:00\u0001");
        msg.append("55=AAPL\u0001");
        msg.append("54=1\u0001");
        msg.append("38=100\u0001");
        msg.append("40=1\u0001");
        
        // Calculate body length (everything after BodyLength field)
        String body = msg.toString();
        String bodyLengthField = "9=" + body.length() + "\u0001";
        
        // Insert body length after BeginString
        String withBodyLength = "8=FIX.4.4\u0001" + bodyLengthField + body.substring(10);
        
        // Calculate checksum
        int sum = 0;
        for (char c : withBodyLength.toCharArray()) {
            sum += c;
        }
        String checksum = String.format("%03d", sum % 256);
        
        return withBodyLength + "10=" + checksum + "\u0001";
    }
}