package com.fixserver.netty;

import com.fixserver.protocol.FIXProtocolHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NettyFIXServer.
 */
@ExtendWith(MockitoExtension.class)
public class NettyFIXServerTest {
    
    @Mock
    private FIXProtocolHandler protocolHandler;
    
    @InjectMocks
    private NettyFIXServer nettyFIXServer;
    
    @Test
    public void testServerConfiguration() {
        // Set test configuration
        ReflectionTestUtils.setField(nettyFIXServer, "nettyPort", 9879);
        ReflectionTestUtils.setField(nettyFIXServer, "bossThreads", 1);
        ReflectionTestUtils.setField(nettyFIXServer, "workerThreads", 2);
        ReflectionTestUtils.setField(nettyFIXServer, "nettyEnabled", true);
        
        assertEquals(9879, nettyFIXServer.getPort());
        assertFalse(nettyFIXServer.isRunning());
    }
    
    @Test
    public void testServerDisabled() {
        ReflectionTestUtils.setField(nettyFIXServer, "nettyEnabled", false);
        
        // Should not start when disabled
        nettyFIXServer.startServer();
        assertFalse(nettyFIXServer.isRunning());
    }
    
    @Test
    public void testStopServerWhenNotRunning() {
        // Should handle stop gracefully when not running
        assertDoesNotThrow(() -> nettyFIXServer.stopServer());
        assertFalse(nettyFIXServer.isRunning());
    }
}