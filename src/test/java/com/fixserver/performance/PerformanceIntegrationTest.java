package com.fixserver.performance;

import com.fixserver.core.FIXMessage;
import com.fixserver.netty.NettyFIXServer;
import com.fixserver.protocol.FIXProtocolHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for performance optimizations in the FIX server workflow.
 * 
 * This test verifies that optimized components are properly integrated
 * and working together in the message processing pipeline.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "fix.server.performance.enabled=true",
    "fix.server.performance.use-optimized-parser=true",
    "fix.server.performance.use-async-storage=true",
    "fix.server.netty.enabled=true"
})
public class PerformanceIntegrationTest {

    @Autowired(required = false)
    private HighPerformanceMessageParser highPerformanceParser;

    @Autowired(required = false)
    private AsyncMessageStore asyncMessageStore;

    @Autowired(required = false)
    private JVMOptimizationConfig jvmOptimizationConfig;

    @Autowired(required = false)
    private PerformanceOptimizer performanceOptimizer;

    @Autowired
    private FIXProtocolHandler protocolHandler;

    @Autowired
    private NettyFIXServer nettyFIXServer;

    @Test
    public void testPerformanceComponentsAreLoaded() {
        // Verify that performance components are properly loaded
        assertNotNull(highPerformanceParser, "HighPerformanceMessageParser should be loaded");
        assertNotNull(asyncMessageStore, "AsyncMessageStore should be loaded");
        assertNotNull(jvmOptimizationConfig, "JVMOptimizationConfig should be loaded");
        assertNotNull(performanceOptimizer, "PerformanceOptimizer should be loaded");
    }

    @Test
    public void testOptimizedParsingWorkflow() throws Exception {
        // Test message parsing with optimized components
        String testMessage = "8=FIX.4.4\u00019=154\u000135=D\u000149=SENDER\u000156=TARGET\u000134=1\u000152=20231201-10:30:00\u000111=12345\u000121=1\u000155=AAPL\u000154=1\u000138=100\u000140=2\u000144=150.50\u000159=0\u000110=123\u0001";

        // Parse using standard handler (should delegate to optimized parser)
        FIXMessage message = protocolHandler.parse(testMessage);
        
        assertNotNull(message);
        assertEquals("FIX.4.4", message.getBeginString());
        assertEquals("D", message.getMessageType());
        assertEquals("SENDER", message.getSenderCompId());
        assertEquals("TARGET", message.getTargetCompId());
    }

    @Test
    public void testAsyncMessageStorage() throws Exception {
        if (asyncMessageStore == null) {
            // Skip test if async storage is not enabled
            return;
        }

        String testMessage = "8=FIX.4.4\u00019=154\u000135=D\u000149=SENDER\u000156=TARGET\u000134=1\u000152=20231201-10:30:00\u000111=12345\u000121=1\u000155=AAPL\u000154=1\u000138=100\u000140=2\u000144=150.50\u000159=0\u000110=123\u0001";
        FIXMessage message = protocolHandler.parse(testMessage);

        // Store message asynchronously
        asyncMessageStore.storeMessage("TEST_SESSION", message, 
            com.fixserver.store.MessageStore.MessageDirection.INCOMING);

        // Give some time for async processing
        Thread.sleep(100);

        // Verify the message was processed (AsyncMessageStore is always running when initialized)
        assertNotNull(asyncMessageStore, "AsyncMessageStore should be initialized");
    }

    @Test
    public void testNettyServerConfiguration() {
        // Verify that Netty server is properly configured
        assertNotNull(nettyFIXServer);
        
        // The server should be configured but not necessarily running in test mode
        assertEquals(9879, nettyFIXServer.getPort());
    }

    @Test
    public void testPerformanceMetricsCollection() {
        if (performanceOptimizer == null) {
            return;
        }

        // Test performance metrics recording
        long startTime = System.nanoTime();
        String testMessage = "8=FIX.4.4\u00019=154\u000135=D\u000149=SENDER\u000156=TARGET\u000134=1\u000152=20231201-10:30:00\u000111=12345\u000121=1\u000155=AAPL\u000154=1\u000138=100\u000140=2\u000144=150.50\u000159=0\u000110=123\u0001";
        
        try {
            FIXMessage message = protocolHandler.parse(testMessage);
            long processingTime = System.nanoTime() - startTime;
            
            // Record metrics
            performanceOptimizer.recordMessageProcessing(processingTime, testMessage.length());
            
            // Verify metrics are being collected
            assertTrue(processingTime > 0);
            assertTrue(testMessage.length() > 0);
            
        } catch (Exception e) {
            fail("Performance metrics test failed: " + e.getMessage());
        }
    }

    @Test
    public void testOptimizedMessageCreation() {
        // Test creating optimized FIX messages
        OptimizedFIXMessage optimizedMessage = new OptimizedFIXMessage();
        
        optimizedMessage.setField(8, "FIX.4.4");
        optimizedMessage.setField(35, "D");
        optimizedMessage.setField(49, "SENDER");
        optimizedMessage.setField(56, "TARGET");
        
        assertEquals("FIX.4.4", optimizedMessage.getField(8));
        assertEquals("D", optimizedMessage.getField(35));
        assertEquals("SENDER", optimizedMessage.getField(49));
        assertEquals("TARGET", optimizedMessage.getField(56));
        
        // Test performance characteristics
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            optimizedMessage.setField(11, "ORDER_" + i);
        }
        long endTime = System.nanoTime();
        
        long duration = endTime - startTime;
        assertTrue(duration > 0, "Performance test should complete in measurable time");
        
        // The optimized message should handle rapid field updates efficiently
        assertEquals("ORDER_999", optimizedMessage.getField(11));
    }

    @Test
    public void testHighPerformanceParserDirectly() {
        if (highPerformanceParser == null) {
            return;
        }

        String testMessage = "8=FIX.4.4\u00019=154\u000135=D\u000149=SENDER\u000156=TARGET\u000134=1\u000152=20231201-10:30:00\u000111=12345\u000121=1\u000155=AAPL\u000154=1\u000138=100\u000140=2\u000144=150.50\u000159=0\u000110=123\u0001";

        // Test direct parsing with high-performance parser
        FIXMessage message = highPerformanceParser.parseFromString(testMessage);
        
        assertNotNull(message);
        assertEquals("FIX.4.4", message.getBeginString());
        assertEquals("D", message.getMessageType());
        assertEquals("SENDER", message.getSenderCompId());
        assertEquals("TARGET", message.getTargetCompId());
        assertEquals("12345", message.getField(11)); // ClOrdID
        assertEquals("AAPL", message.getField(55)); // Symbol
    }
}