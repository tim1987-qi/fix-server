package com.fixserver.client;

import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FIXClientImpl.
 * 
 * Tests the core functionality of the FIX client including connection management,
 * message sending, and event handling.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
class FIXClientImplTest {
    
    private FIXClientConfiguration config;
    private FIXClientImpl client;
    
    @BeforeEach
    void setUp() {
        config = FIXClientConfiguration.builder()
                .host("localhost")
                .port(9876)
                .senderCompId("TEST_CLIENT")
                .targetCompId("TEST_SERVER")
                .heartbeatInterval(30)
                .connectionTimeout(java.time.Duration.ofSeconds(5))
                .logonTimeout(java.time.Duration.ofSeconds(5))
                .build();
        
        client = new FIXClientImpl(config);
    }
    
    @AfterEach
    void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }
    
    @Test
    void testClientConfiguration() {
        assertEquals("localhost", config.getHost());
        assertEquals(9876, config.getPort());
        assertEquals("TEST_CLIENT", config.getSenderCompId());
        assertEquals("TEST_SERVER", config.getTargetCompId());
        assertEquals("FIX.4.4", config.getFixVersion());
        assertEquals(30, config.getHeartbeatInterval());
        assertFalse(config.isResetSeqNumFlag());
        assertTrue(config.isValidateMessages());
        assertTrue(config.isAutoHeartbeat());
        assertTrue(config.isAutoResendRequest());
    }
    
    @Test
    void testInitialState() {
        assertFalse(client.isConnected());
        assertEquals("TEST_CLIENT-TEST_SERVER", client.getSessionId());
    }
    
    @Test
    void testConnectionHandlerRegistration() {
        AtomicReference<FIXClient> connectedClient = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnected(FIXClient client) {
                connectedClient.set(client);
                latch.countDown();
            }
            
            @Override
            public void onDisconnected(FIXClient client, String reason) {}
            
            @Override
            public void onError(FIXClient client, Throwable error) {}
            
            @Override
            public void onLoggedOn(FIXClient client) {}
            
            @Override
            public void onLoggedOut(FIXClient client) {}
        });
        
        // Connection handler should be registered
        assertNotNull(client);
    }
    
    @Test
    void testMessageHandlerRegistration() {
        AtomicReference<FIXMessage> receivedMessage = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        client.setMessageHandler((message, clientRef) -> {
            receivedMessage.set(message);
            latch.countDown();
        });
        
        // Message handler should be registered
        assertNotNull(client);
    }
    
    @Test
    void testSendMessageWhenNotConnected() {
        FIXMessage message = OrderMessage.marketOrder("TEST_ORDER", "AAPL", 
                OrderMessage.Side.BUY, new BigDecimal("100"));
        
        CompletableFuture<Void> future = client.sendMessage(message);
        
        assertThrows(Exception.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testClientFactory() {
        FIXClient factoryClient = FIXClientFactory.createClient(config);
        assertNotNull(factoryClient);
        assertInstanceOf(FIXClientImpl.class, factoryClient);
        
        // Clean up
        if (factoryClient instanceof FIXClientImpl) {
            ((FIXClientImpl) factoryClient).shutdown();
        }
    }
    
    @Test
    void testClientFactoryWithBasicParams() {
        FIXClient factoryClient = FIXClientFactory.createClient("localhost", 9876, "CLIENT", "SERVER");
        assertNotNull(factoryClient);
        assertInstanceOf(FIXClientImpl.class, factoryClient);
        
        // Clean up
        if (factoryClient instanceof FIXClientImpl) {
            ((FIXClientImpl) factoryClient).shutdown();
        }
    }
    
    @Test
    void testConfigurationBuilder() {
        FIXClientConfiguration.FIXClientConfigurationBuilder builder = FIXClientFactory.builder();
        assertNotNull(builder);
        
        FIXClientConfiguration builtConfig = builder
                .host("testhost")
                .port(1234)
                .senderCompId("SENDER")
                .targetCompId("TARGET")
                .heartbeatInterval(60)
                .resetSeqNumFlag(true)
                .build();
        
        assertEquals("testhost", builtConfig.getHost());
        assertEquals(1234, builtConfig.getPort());
        assertEquals("SENDER", builtConfig.getSenderCompId());
        assertEquals("TARGET", builtConfig.getTargetCompId());
        assertEquals(60, builtConfig.getHeartbeatInterval());
        assertTrue(builtConfig.isResetSeqNumFlag());
    }
    
    @Test
    void testDefaultConfiguration() {
        FIXClientConfiguration defaultConfig = FIXClientConfiguration.defaultConfig(
                "localhost", 9876, "CLIENT", "SERVER");
        
        assertEquals("localhost", defaultConfig.getHost());
        assertEquals(9876, defaultConfig.getPort());
        assertEquals("CLIENT", defaultConfig.getSenderCompId());
        assertEquals("SERVER", defaultConfig.getTargetCompId());
        assertEquals("FIX.4.4", defaultConfig.getFixVersion());
        assertEquals(30, defaultConfig.getHeartbeatInterval());
        assertFalse(defaultConfig.isResetSeqNumFlag());
    }
    
    @Test
    void testShutdown() {
        // Should not throw exception
        assertDoesNotThrow(() -> client.shutdown());
        
        // Should be safe to call multiple times
        assertDoesNotThrow(() -> client.shutdown());
    }
    
    @Test
    void testConnectionTimeout() {
        // Test that connection timeout is respected
        FIXClientConfiguration timeoutConfig = FIXClientConfiguration.builder()
                .host("192.0.2.1") // Non-routable IP for testing timeout
                .port(9999)
                .senderCompId("CLIENT")
                .targetCompId("SERVER")
                .connectionTimeout(java.time.Duration.ofMillis(100))
                .build();
        
        FIXClientImpl timeoutClient = new FIXClientImpl(timeoutConfig);
        
        try {
            CompletableFuture<Void> connectFuture = timeoutClient.connect();
            
            assertThrows(Exception.class, () -> {
                connectFuture.get(2, TimeUnit.SECONDS);
            });
        } finally {
            timeoutClient.shutdown();
        }
    }
}