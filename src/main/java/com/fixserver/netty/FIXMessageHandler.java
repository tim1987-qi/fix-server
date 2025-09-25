package com.fixserver.netty;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Netty handler for processing FIX protocol messages.
 * 
 * This handler processes incoming FIX messages and generates appropriate
 * responses. It maintains session state and handles the FIX protocol
 * message flow including logon, logout, heartbeats, and business messages.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class FIXMessageHandler extends SimpleChannelInboundHandler<String> {
    
    private final FIXProtocolHandler protocolHandler;
    private final ConcurrentMap<String, NettyFIXSession> sessions = new ConcurrentHashMap<>();
    
    public FIXMessageHandler(FIXProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientAddress = ctx.channel().remoteAddress().toString();
        log.info("New FIX client connected: {}", clientAddress);
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientAddress = ctx.channel().remoteAddress().toString();
        log.info("FIX client disconnected: {}", clientAddress);
        
        // Clean up session
        String channelId = ctx.channel().id().asShortText();
        NettyFIXSession session = sessions.remove(channelId);
        if (session != null) {
            log.info("Cleaned up session: {}", session.getSessionId());
        }
        
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String rawMessage) throws Exception {
        String clientAddress = ctx.channel().remoteAddress().toString();
        log.debug("Received raw message from {}: {}", clientAddress, rawMessage.replace('\u0001', '|'));
        
        try {
            // Parse the FIX message
            FIXMessage message = protocolHandler.parse(rawMessage);
            log.info("Received FIX message from {}: Type={}, Sender={}, Target={}", 
                    clientAddress, message.getMessageType(), 
                    message.getSenderCompId(), message.getTargetCompId());
            
            // Get or create session
            String channelId = ctx.channel().id().asShortText();
            NettyFIXSession session = getOrCreateSession(channelId, message, ctx);
            
            // Process message based on type
            String messageType = message.getMessageType();
            
            switch (messageType) {
                case "A": // Logon
                    handleLogon(message, session, ctx);
                    break;
                case "5": // Logout
                    handleLogout(message, session, ctx);
                    break;
                case "0": // Heartbeat
                    handleHeartbeat(message, session, ctx);
                    break;
                case "1": // Test Request
                    handleTestRequest(message, session, ctx);
                    break;
                case "D": // New Order Single
                    handleNewOrder(message, session, ctx);
                    break;
                default:
                    handleUnsupportedMessage(message, session, ctx);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error processing FIX message from {}: {}", clientAddress, rawMessage.replace('\u0001', '|'), e);
            sendReject(ctx, "Invalid message format: " + e.getMessage());
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String clientAddress = ctx.channel().remoteAddress().toString();
        log.error("Exception in FIX message handler for client {}", clientAddress, cause);
        ctx.close();
    }
    
    private NettyFIXSession getOrCreateSession(String channelId, FIXMessage message, ChannelHandlerContext ctx) {
        return sessions.computeIfAbsent(channelId, k -> {
            String sessionId = message.getSenderCompId() + "-" + message.getTargetCompId();
            return new NettyFIXSession(sessionId, ctx);
        });
    }
    
    private void handleLogon(FIXMessage logonMessage, NettyFIXSession session, ChannelHandlerContext ctx) throws Exception {
        String senderCompId = logonMessage.getSenderCompId();
        String targetCompId = logonMessage.getTargetCompId();
        
        // Create logon response
        FIXMessage logonResponse = new FIXMessageImpl();
        logonResponse.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logonResponse.setField(FIXMessage.MESSAGE_TYPE, "A"); // Logon
        logonResponse.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        logonResponse.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        logonResponse.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        logonResponse.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        logonResponse.setField(98, "0"); // EncryptMethod (None)
        logonResponse.setField(108, "30"); // HeartBtInt
        
        session.setLoggedOn(true);
        sendMessage(ctx, logonResponse);
        
        log.info("Client {} logged on with session {}", ctx.channel().remoteAddress(), session.getSessionId());
    }
    
    private void handleLogout(FIXMessage logoutMessage, NettyFIXSession session, ChannelHandlerContext ctx) throws Exception {
        String senderCompId = logoutMessage.getSenderCompId();
        String targetCompId = logoutMessage.getTargetCompId();
        
        // Create logout response
        FIXMessage logoutResponse = new FIXMessageImpl();
        logoutResponse.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logoutResponse.setField(FIXMessage.MESSAGE_TYPE, "5"); // Logout
        logoutResponse.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        logoutResponse.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        logoutResponse.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "2");
        logoutResponse.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        logoutResponse.setField(58, "Goodbye"); // Text
        
        sendMessage(ctx, logoutResponse);
        session.setLoggedOn(false);
        
        log.info("Client {} logged out", ctx.channel().remoteAddress());
        
        // Close connection after logout
        ctx.close();
    }
    
    private void handleHeartbeat(FIXMessage heartbeat, NettyFIXSession session, ChannelHandlerContext ctx) throws Exception {
        log.debug("Received heartbeat from {}", ctx.channel().remoteAddress());
        session.updateLastActivity();
    }
    
    private void handleTestRequest(FIXMessage testRequest, NettyFIXSession session, ChannelHandlerContext ctx) throws Exception {
        String senderCompId = testRequest.getSenderCompId();
        String targetCompId = testRequest.getTargetCompId();
        String testReqId = testRequest.getField(112); // TestReqID
        
        // Create heartbeat response
        FIXMessage heartbeatResponse = new FIXMessageImpl();
        heartbeatResponse.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        heartbeatResponse.setField(FIXMessage.MESSAGE_TYPE, "0"); // Heartbeat
        heartbeatResponse.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        heartbeatResponse.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        heartbeatResponse.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "3");
        heartbeatResponse.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        if (testReqId != null && !testReqId.isEmpty()) {
            heartbeatResponse.setField(112, testReqId); // TestReqID
        }
        
        sendMessage(ctx, heartbeatResponse);
        log.debug("Sent heartbeat response to test request from {}", ctx.channel().remoteAddress());
    }
    
    private void handleNewOrder(FIXMessage orderMessage, NettyFIXSession session, ChannelHandlerContext ctx) throws Exception {
        String senderCompId = orderMessage.getSenderCompId();
        String targetCompId = orderMessage.getTargetCompId();
        String clOrdId = orderMessage.getField(11); // ClOrdID
        String symbol = orderMessage.getField(55); // Symbol
        String side = orderMessage.getField(54); // Side
        String orderQty = orderMessage.getField(38); // OrderQty
        
        // Create execution report (order accepted)
        FIXMessage executionReport = new FIXMessageImpl();
        executionReport.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        executionReport.setField(FIXMessage.MESSAGE_TYPE, "8"); // Execution Report
        executionReport.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        executionReport.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        executionReport.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "4");
        executionReport.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        // Order-specific fields
        executionReport.setField(11, clOrdId); // ClOrdID
        executionReport.setField(17, "EXEC_" + System.currentTimeMillis()); // ExecID
        executionReport.setField(20, "0"); // ExecTransType
        executionReport.setField(37, "ORDER_" + System.currentTimeMillis()); // OrderID
        executionReport.setField(39, "0"); // OrdStatus (New)
        executionReport.setField(54, side); // Side
        executionReport.setField(55, symbol); // Symbol
        executionReport.setField(150, "0"); // ExecType (New)
        executionReport.setField(151, orderQty); // LeavesQty
        
        sendMessage(ctx, executionReport);
        log.info("Processed order {} for symbol {} from {}", clOrdId, symbol, ctx.channel().remoteAddress());
    }
    
    private void handleUnsupportedMessage(FIXMessage message, NettyFIXSession session, ChannelHandlerContext ctx) throws Exception {
        String messageType = message.getMessageType();
        log.warn("Unsupported message type {} from {}", messageType, ctx.channel().remoteAddress());
        
        sendBusinessReject(ctx, message.getSenderCompId(), message.getTargetCompId(), 
                "Message type not supported: " + messageType);
    }
    
    private void sendMessage(ChannelHandlerContext ctx, FIXMessage message) throws Exception {
        String fixString = message.toFixString();
        ctx.writeAndFlush(fixString);
        log.debug("Sent FIX message: {}", fixString.replace('\u0001', '|'));
    }
    
    private void sendReject(ChannelHandlerContext ctx, String reason) throws Exception {
        FIXMessage rejectMessage = new FIXMessageImpl();
        rejectMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        rejectMessage.setField(FIXMessage.MESSAGE_TYPE, "3"); // Reject
        rejectMessage.setField(FIXMessage.SENDER_COMP_ID, "SERVER");
        rejectMessage.setField(FIXMessage.TARGET_COMP_ID, "CLIENT");
        rejectMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        rejectMessage.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        rejectMessage.setField(45, "0"); // RefSeqNum
        rejectMessage.setField(58, reason); // Text
        
        sendMessage(ctx, rejectMessage);
    }
    
    private void sendBusinessReject(ChannelHandlerContext ctx, String senderCompId, String targetCompId, String reason) throws Exception {
        FIXMessage rejectMessage = new FIXMessageImpl();
        rejectMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        rejectMessage.setField(FIXMessage.MESSAGE_TYPE, "j"); // Business Message Reject
        rejectMessage.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        rejectMessage.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        rejectMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "5");
        rejectMessage.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        rejectMessage.setField(45, "0"); // RefSeqNum
        rejectMessage.setField(372, "D"); // RefMsgType
        rejectMessage.setField(380, "1"); // BusinessRejectReason
        rejectMessage.setField(58, reason); // Text
        
        sendMessage(ctx, rejectMessage);
    }
}