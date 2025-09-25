package com.fixserver.netty;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * Netty client handler for processing FIX messages from the server.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class NettyFIXClientHandler extends SimpleChannelInboundHandler<String> {
    
    private final CountDownLatch logonLatch;
    private final FIXProtocolHandler protocolHandler = new FIXProtocolHandler();
    
    public NettyFIXClientHandler(CountDownLatch logonLatch) {
        this.logonLatch = logonLatch;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connected to server: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Disconnected from server: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String rawMessage) throws Exception {
        log.debug("Received raw message: {}", rawMessage.replace('\u0001', '|'));
        
        try {
            // Parse the FIX message
            FIXMessage message = protocolHandler.parse(rawMessage);
            
            String messageType = message.getMessageType();
            log.info("Received FIX message: Type={}, Sender={}, Target={}", 
                    messageType, message.getSenderCompId(), message.getTargetCompId());
            
            switch (messageType) {
                case "A": // Logon
                    handleLogonResponse(message);
                    break;
                case "5": // Logout
                    handleLogoutResponse(message);
                    break;
                case "0": // Heartbeat
                    handleHeartbeat(message);
                    break;
                case "8": // Execution Report
                    handleExecutionReport(message);
                    break;
                case "3": // Reject
                    handleReject(message);
                    break;
                case "j": // Business Message Reject
                    handleBusinessReject(message);
                    break;
                default:
                    log.warn("Unhandled message type: {}", messageType);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error processing message: {}", rawMessage.replace('\u0001', '|'), e);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in client handler", cause);
        ctx.close();
    }
    
    private void handleLogonResponse(FIXMessage message) {
        log.info("Received logon response from server");
        logonLatch.countDown();
    }
    
    private void handleLogoutResponse(FIXMessage message) {
        String text = message.getField(58); // Text
        log.info("Received logout response: {}", text != null ? text : "No reason provided");
    }
    
    private void handleHeartbeat(FIXMessage message) {
        String testReqId = message.getField(112); // TestReqID
        if (testReqId != null) {
            log.debug("Received heartbeat response for test request: {}", testReqId);
        } else {
            log.debug("Received heartbeat from server");
        }
    }
    
    private void handleExecutionReport(FIXMessage message) {
        String clOrdId = message.getField(11); // ClOrdID
        String execId = message.getField(17); // ExecID
        String ordStatus = message.getField(39); // OrdStatus
        String symbol = message.getField(55); // Symbol
        String side = message.getField(54); // Side
        String orderQty = message.getField(38); // OrderQty
        String leavesQty = message.getField(151); // LeavesQty
        
        System.out.println("\n=== Execution Report ===");
        System.out.println("Order ID: " + clOrdId);
        System.out.println("Execution ID: " + execId);
        System.out.println("Symbol: " + symbol);
        System.out.println("Side: " + (side != null && side.equals("1") ? "BUY" : "SELL"));
        System.out.println("Quantity: " + orderQty);
        System.out.println("Leaves Qty: " + leavesQty);
        System.out.println("Status: " + getOrderStatusDescription(ordStatus));
        System.out.println("========================\n");
        
        log.info("Execution report: Order={}, Symbol={}, Status={}", clOrdId, symbol, ordStatus);
    }
    
    private void handleReject(FIXMessage message) {
        String text = message.getField(58); // Text
        String refSeqNum = message.getField(45); // RefSeqNum
        
        System.out.println("\n=== Message Reject ===");
        System.out.println("Reference Seq Num: " + refSeqNum);
        System.out.println("Reason: " + text);
        System.out.println("======================\n");
        
        log.warn("Message rejected: RefSeqNum={}, Reason={}", refSeqNum, text);
    }
    
    private void handleBusinessReject(FIXMessage message) {
        String text = message.getField(58); // Text
        String refMsgType = message.getField(372); // RefMsgType
        String businessRejectReason = message.getField(380); // BusinessRejectReason
        
        System.out.println("\n=== Business Message Reject ===");
        System.out.println("Reference Message Type: " + refMsgType);
        System.out.println("Business Reject Reason: " + businessRejectReason);
        System.out.println("Text: " + text);
        System.out.println("===============================\n");
        
        log.warn("Business message rejected: RefMsgType={}, Reason={}, Text={}", 
                refMsgType, businessRejectReason, text);
    }
    
    private String getOrderStatusDescription(String ordStatus) {
        if (ordStatus == null) return "Unknown";
        
        switch (ordStatus) {
            case "0": return "New";
            case "1": return "Partially Filled";
            case "2": return "Filled";
            case "4": return "Canceled";
            case "6": return "Pending Cancel";
            case "8": return "Rejected";
            case "A": return "Pending New";
            case "C": return "Expired";
            case "E": return "Pending Replace";
            default: return "Unknown (" + ordStatus + ")";
        }
    }
}