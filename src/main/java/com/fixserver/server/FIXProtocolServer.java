package com.fixserver.server;

import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.FIXProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FIX Protocol Server that listens for incoming FIX connections.
 * 
 * This server accepts TCP connections on the configured FIX port and handles
 * FIX protocol messages from clients.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class FIXProtocolServer {
    
    @Value("${fix.server.port:9878}")
    private int fixPort;
    
    @Autowired
    private FIXProtocolHandler protocolHandler;
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * Starts the FIX protocol server when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        if (running.get()) {
            log.warn("FIX Protocol Server is already running");
            return;
        }
        
        try {
            serverSocket = new ServerSocket(fixPort);
            executorService = Executors.newCachedThreadPool();
            running.set(true);
            
            log.info("FIX Protocol Server started on port {}", fixPort);
            log.info("Waiting for FIX client connections...");
            
            // Accept connections in a separate thread to avoid blocking the main thread
            executorService.submit(() -> {
                while (running.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        log.info("Accepted FIX client connection from {}", 
                                clientSocket.getRemoteSocketAddress());
                        
                        // Handle each client connection in a separate thread
                        executorService.submit(() -> handleClientConnection(clientSocket));
                        
                    } catch (Exception e) {
                        if (running.get()) {
                            log.error("Error accepting client connection", e);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to start FIX Protocol Server on port {}", fixPort, e);
            running.set(false);
        }
    }
    
    /**
     * Handles individual client connections.
     */
    private void handleClientConnection(Socket clientSocket) {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        log.info("Handling FIX client connection from {}", clientAddress);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            
            String sessionId = null;
            StringBuilder messageBuffer = new StringBuilder();
            
            // Read messages from client
            int ch;
            while ((ch = reader.read()) != -1 && running.get()) {
                messageBuffer.append((char) ch);
                
                // Check for complete message (ends with SOH after checksum field)
                if (ch == '\u0001') {
                    String currentMessage = messageBuffer.toString();
                    
                    // Check if this is a complete FIX message by looking for checksum field at the end
                    if (isCompleteFixMessage(currentMessage)) {
                        messageBuffer.setLength(0);
                        
                        try {
                            // Parse the FIX message
                            FIXMessage message = protocolHandler.parse(currentMessage);
                            log.info("Received FIX message from {}: Type={}, Sender={}, Target={}", 
                                    clientAddress, message.getMessageType(), 
                                    message.getSenderCompId(), message.getTargetCompId());
                            
                            // Handle the message based on type
                            String messageType = message.getMessageType();
                            
                            if ("A".equals(messageType)) {
                                // Logon message
                                sessionId = handleLogon(message, writer);
                                log.info("Client {} logged on with session {}", clientAddress, sessionId);
                            } else if ("5".equals(messageType)) {
                                // Logout message
                                handleLogout(message, writer);
                                log.info("Client {} logged out", clientAddress);
                                break;
                            } else if ("0".equals(messageType)) {
                                // Heartbeat
                                log.debug("Received heartbeat from {}", clientAddress);
                            } else if ("1".equals(messageType)) {
                                // Test Request
                                handleTestRequest(message, writer);
                                log.debug("Handled test request from {}", clientAddress);
                            } else if ("D".equals(messageType)) {
                                // New Order Single
                                handleNewOrder(message, writer);
                                log.info("Handled new order from {}", clientAddress);
                            } else {
                                // For other message types, send a simple acknowledgment
                                sendBusinessReject(writer, message.getSenderCompId(), message.getTargetCompId(), 
                                        "Message type not supported: " + messageType);
                            }
                            
                        } catch (Exception e) {
                            log.error("Error processing FIX message from {}: {}", clientAddress, currentMessage.replace('\u0001', '|'), e);
                            try {
                                sendReject(writer, "Invalid message format");
                            } catch (Exception ex) {
                                log.error("Error sending reject message", ex);
                            }
                        }
                    }
                    // If not complete, continue reading more data
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling client connection from {}", clientAddress, e);
        } finally {
            try {
                clientSocket.close();
                log.info("Closed connection to client {}", clientAddress);
            } catch (Exception e) {
                log.error("Error closing client socket", e);
            }
        }
    }   
 
    /**
     * Checks if the current buffer contains a complete FIX message.
     * A complete FIX message should have BeginString, BodyLength, and end with Checksum field.
     */
    private boolean isCompleteFixMessage(String messageBuffer) {
        if (messageBuffer == null || messageBuffer.isEmpty()) {
            return false;
        }
        
        // Split by SOH to get fields
        String[] fields = messageBuffer.split("\u0001");
        if (fields.length < 4) { // At minimum: BeginString, BodyLength, MessageType, Checksum
            return false;
        }
        
        // Check if message starts with BeginString (tag 8)
        if (!fields[0].startsWith("8=")) {
            return false;
        }
        
        // Check if second field is BodyLength (tag 9)
        if (fields.length < 2 || !fields[1].startsWith("9=")) {
            return false;
        }
        
        // Check if the last non-empty field is Checksum (tag 10)
        String lastField = null;
        for (int i = fields.length - 1; i >= 0; i--) {
            if (!fields[i].isEmpty()) {
                lastField = fields[i];
                break;
            }
        }
        
        return lastField != null && lastField.startsWith("10=");
    }
    
    /**
     * Handles logon message and sends logon response.
     */
    private String handleLogon(FIXMessage logonMessage, BufferedWriter writer) throws Exception {
        String senderCompId = logonMessage.getSenderCompId();
        String targetCompId = logonMessage.getTargetCompId();
        String sessionId = senderCompId + "-" + targetCompId;
        
        // Create logon response using FIXMessage
        FIXMessage logonResponse = new com.fixserver.core.FIXMessageImpl();
        logonResponse.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logonResponse.setField(FIXMessage.MESSAGE_TYPE, "A"); // Logon
        logonResponse.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        logonResponse.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        logonResponse.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        logonResponse.setField(FIXMessage.SENDING_TIME, java.time.LocalDateTime.now().toString());
        logonResponse.setField(98, "0"); // EncryptMethod (None)
        logonResponse.setField(108, "30"); // HeartBtInt
        
        // Send the properly formatted message
        String fixString = logonResponse.toFixString();
        writer.write(fixString);
        writer.flush();
        
        log.info("Sent logon response to session {}: {}", sessionId, fixString.replace('\u0001', '|'));
        return sessionId;
    }
    
    /**
     * Handles logout message and sends logout response.
     */
    private void handleLogout(FIXMessage logoutMessage, BufferedWriter writer) throws Exception {
        String senderCompId = logoutMessage.getSenderCompId();
        String targetCompId = logoutMessage.getTargetCompId();
        
        // Create logout response using FIXMessage
        FIXMessage logoutResponse = new com.fixserver.core.FIXMessageImpl();
        logoutResponse.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logoutResponse.setField(FIXMessage.MESSAGE_TYPE, "5"); // Logout
        logoutResponse.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        logoutResponse.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        logoutResponse.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "2");
        logoutResponse.setField(FIXMessage.SENDING_TIME, java.time.LocalDateTime.now().toString());
        logoutResponse.setField(58, "Goodbye"); // Text
        
        // Send the properly formatted message
        String fixString = logoutResponse.toFixString();
        writer.write(fixString);
        writer.flush();
        
        log.info("Sent logout response: {}", fixString.replace('\u0001', '|'));
    }
    
    /**
     * Handles test request and sends heartbeat response.
     */
    private void handleTestRequest(FIXMessage testRequest, BufferedWriter writer) throws Exception {
        String senderCompId = testRequest.getSenderCompId();
        String targetCompId = testRequest.getTargetCompId();
        String testReqId = testRequest.getField(112); // TestReqID
        
        // Create heartbeat response using FIXMessage
        FIXMessage heartbeatResponse = new com.fixserver.core.FIXMessageImpl();
        heartbeatResponse.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        heartbeatResponse.setField(FIXMessage.MESSAGE_TYPE, "0"); // Heartbeat
        heartbeatResponse.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        heartbeatResponse.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        heartbeatResponse.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "3");
        heartbeatResponse.setField(FIXMessage.SENDING_TIME, java.time.LocalDateTime.now().toString());
        
        if (testReqId != null && !testReqId.isEmpty()) {
            heartbeatResponse.setField(112, testReqId); // TestReqID
        }
        
        // Send the properly formatted message
        String fixString = heartbeatResponse.toFixString();
        writer.write(fixString);
        writer.flush();
        
        log.debug("Sent heartbeat response to test request: {}", fixString.replace('\u0001', '|'));
    }
    
    /**
     * Handles new order and sends execution report.
     */
    private void handleNewOrder(FIXMessage orderMessage, BufferedWriter writer) throws Exception {
        String senderCompId = orderMessage.getSenderCompId();
        String targetCompId = orderMessage.getTargetCompId();
        String clOrdId = orderMessage.getField(11); // ClOrdID
        String symbol = orderMessage.getField(55); // Symbol
        String side = orderMessage.getField(54); // Side
        String orderQty = orderMessage.getField(38); // OrderQty
        
        // Create execution report (order accepted) using FIXMessage
        FIXMessage executionReport = new com.fixserver.core.FIXMessageImpl();
        executionReport.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        executionReport.setField(FIXMessage.MESSAGE_TYPE, "8"); // Execution Report
        executionReport.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        executionReport.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        executionReport.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "4");
        executionReport.setField(FIXMessage.SENDING_TIME, java.time.LocalDateTime.now().toString());
        
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
        
        // Send the properly formatted message
        String fixString = executionReport.toFixString();
        writer.write(fixString);
        writer.flush();
        
        log.info("Sent execution report for order {} on symbol {}: {}", clOrdId, symbol, fixString.replace('\u0001', '|'));
    }
    
    /**
     * Sends a reject message.
     */
    private void sendReject(BufferedWriter writer, String reason) throws Exception {
        // Create reject message using FIXMessage
        FIXMessage rejectMessage = new com.fixserver.core.FIXMessageImpl();
        rejectMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        rejectMessage.setField(FIXMessage.MESSAGE_TYPE, "3"); // Reject
        rejectMessage.setField(FIXMessage.SENDER_COMP_ID, "SERVER");
        rejectMessage.setField(FIXMessage.TARGET_COMP_ID, "CLIENT");
        rejectMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        rejectMessage.setField(FIXMessage.SENDING_TIME, java.time.LocalDateTime.now().toString());
        rejectMessage.setField(45, "0"); // RefSeqNum
        rejectMessage.setField(58, reason); // Text
        
        // Send the properly formatted message
        String fixString = rejectMessage.toFixString();
        writer.write(fixString);
        writer.flush();
        
        log.info("Sent reject message: {}", fixString.replace('\u0001', '|'));
    }
    
    /**
     * Sends a business message reject.
     */
    private void sendBusinessReject(BufferedWriter writer, String senderCompId, String targetCompId, String reason) throws Exception {
        // Create business reject message using FIXMessage
        FIXMessage rejectMessage = new com.fixserver.core.FIXMessageImpl();
        rejectMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        rejectMessage.setField(FIXMessage.MESSAGE_TYPE, "j"); // Business Message Reject
        rejectMessage.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
        rejectMessage.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
        rejectMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "5");
        rejectMessage.setField(FIXMessage.SENDING_TIME, java.time.LocalDateTime.now().toString());
        rejectMessage.setField(45, "0"); // RefSeqNum
        rejectMessage.setField(372, "D"); // RefMsgType
        rejectMessage.setField(380, "1"); // BusinessRejectReason
        rejectMessage.setField(58, reason); // Text
        
        // Send the properly formatted message
        String fixString = rejectMessage.toFixString();
        writer.write(fixString);
        writer.flush();
        
        log.info("Sent business reject message: {}", fixString.replace('\u0001', '|'));
    }
    
    /**
     * Stops the FIX protocol server.
     */
    @PreDestroy
    public void stopServer() {
        if (!running.get()) {
            return;
        }
        
        log.info("Stopping FIX Protocol Server...");
        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            log.error("Error closing server socket", e);
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        log.info("FIX Protocol Server stopped");
    }
}