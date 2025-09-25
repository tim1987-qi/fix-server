package com.fixserver.client;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.protocol.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of FIX client for connecting to FIX servers.
 * 
 * This client provides full FIX protocol support including:
 * - Connection management with automatic reconnection
 * - Session management with logon/logout
 * - Message sending and receiving
 * - Heartbeat management
 * - Sequence number management
 * - Error handling and recovery
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class FIXClientImpl implements FIXClient {
    
    private final FIXClientConfiguration config;
    private final FIXProtocolHandler protocolHandler;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Connection state
    private final AtomicReference<Socket> socket = new AtomicReference<>();
    private final AtomicReference<BufferedWriter> writer = new AtomicReference<>();
    private final AtomicReference<BufferedReader> reader = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean loggedOn = new AtomicBoolean(false);
    
    // Session management
    private final AtomicInteger outgoingSeqNum = new AtomicInteger(1);
    private final AtomicInteger incomingSeqNum = new AtomicInteger(1);
    private String sessionId;
    
    // Message handling
    private volatile FIXClientMessageHandler messageHandler;
    private volatile FIXClientConnectionHandler connectionHandler;
    
    // Heartbeat management
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile long lastHeartbeatTime;
    
    // Response waiting
    private final ConcurrentHashMap<String, CompletableFuture<FIXMessage>> pendingResponses = new ConcurrentHashMap<>();
    
    /**
     * Creates a new FIX client with the specified configuration.
     * 
     * @param config the client configuration
     */
    public FIXClientImpl(FIXClientConfiguration config) {
        this.config = config;
        this.protocolHandler = new FIXProtocolHandler();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "FIXClient-" + config.getSenderCompId());
            t.setDaemon(true);
            return t;
        });
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "FIXClient-Scheduler-" + config.getSenderCompId());
            t.setDaemon(true);
            return t;
        });
        
        this.sessionId = config.getSenderCompId() + "-" + config.getTargetCompId();
    }
    
    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                doConnect();
            } catch (Exception e) {
                log.error("Failed to connect to FIX server", e);
                throw new RuntimeException(new FIXClientException("Connection failed", e));
            }
        }, executorService);
    }
    
    private void doConnect() throws Exception {
        log.info("Connecting to FIX server at {}:{}", config.getHost(), config.getPort());
        
        // Create socket connection
        Socket newSocket = new Socket();
        newSocket.connect(new java.net.InetSocketAddress(config.getHost(), config.getPort()), 
                         (int) config.getConnectionTimeout().toMillis());
        
        // Set up I/O streams
        BufferedWriter newWriter = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream()));
        BufferedReader newReader = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
        
        // Update connection state
        socket.set(newSocket);
        writer.set(newWriter);
        reader.set(newReader);
        connected.set(true);
        
        log.info("Connected to FIX server, starting message reader");
        
        // Start message reader thread
        executorService.submit(this::messageReaderLoop);
        
        // Notify connection handler
        if (connectionHandler != null) {
            connectionHandler.onConnected(this);
        }
        
        // Send logon message
        sendLogon();
    }
    
    private void sendLogon() throws Exception {
        log.info("Sending logon message");
        
        FIXMessage logonMessage = new FIXMessageImpl();
        logonMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logonMessage.setField(FIXMessage.MESSAGE_TYPE, MessageType.LOGON.getValue());
        logonMessage.setField(FIXMessage.SENDER_COMP_ID, config.getSenderCompId());
        logonMessage.setField(FIXMessage.TARGET_COMP_ID, config.getTargetCompId());
        logonMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(outgoingSeqNum.getAndIncrement()));
        logonMessage.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        // Add logon-specific fields
        logonMessage.setField(98, "0"); // EncryptMethod (None)
        logonMessage.setField(108, String.valueOf(config.getHeartbeatInterval())); // HeartBtInt
        
        if (config.isResetSeqNumFlag()) {
            logonMessage.setField(141, "Y"); // ResetSeqNumFlag
        }
        
        if (config.getUsername() != null) {
            logonMessage.setField(553, config.getUsername()); // Username
        }
        
        if (config.getPassword() != null) {
            logonMessage.setField(554, config.getPassword()); // Password
        }
        
        sendMessageInternal(logonMessage);
    }
    
    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                doDisconnect("Client requested disconnect");
            } catch (Exception e) {
                log.error("Error during disconnect", e);
            }
        }, executorService);
    }
    
    private void doDisconnect(String reason) throws Exception {
        log.info("Disconnecting from FIX server: {}", reason);
        
        if (loggedOn.get()) {
            sendLogout(reason);
        }
        
        // Stop heartbeat
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        
        // Close connection
        connected.set(false);
        loggedOn.set(false);
        
        Socket currentSocket = socket.getAndSet(null);
        if (currentSocket != null && !currentSocket.isClosed()) {
            currentSocket.close();
        }
        
        writer.set(null);
        reader.set(null);
        
        // Notify connection handler
        if (connectionHandler != null) {
            connectionHandler.onDisconnected(this, reason);
        }
        
        log.info("Disconnected from FIX server");
    }
    
    private void sendLogout(String reason) throws Exception {
        log.info("Sending logout message: {}", reason);
        
        FIXMessage logoutMessage = new FIXMessageImpl();
        logoutMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logoutMessage.setField(FIXMessage.MESSAGE_TYPE, MessageType.LOGOUT.getValue());
        logoutMessage.setField(FIXMessage.SENDER_COMP_ID, config.getSenderCompId());
        logoutMessage.setField(FIXMessage.TARGET_COMP_ID, config.getTargetCompId());
        logoutMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(outgoingSeqNum.getAndIncrement()));
        logoutMessage.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        if (reason != null) {
            logoutMessage.setField(58, reason); // Text
        }
        
        sendMessageInternal(logoutMessage);
    }
    
    @Override
    public CompletableFuture<Void> sendMessage(FIXMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!loggedOn.get()) {
                    throw new FIXClientException("Client is not logged on");
                }
                
                // Set sequence number and session info
                message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(outgoingSeqNum.getAndIncrement()));
                message.setField(FIXMessage.SENDER_COMP_ID, config.getSenderCompId());
                message.setField(FIXMessage.TARGET_COMP_ID, config.getTargetCompId());
                message.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
                
                sendMessageInternal(message);
                
            } catch (Exception e) {
                log.error("Failed to send message", e);
                throw new RuntimeException(new FIXClientException("Failed to send message", e));
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<FIXMessage> sendAndWaitForResponse(FIXMessage message) {
        CompletableFuture<FIXMessage> responseFuture = new CompletableFuture<>();
        
        // Generate a unique correlation ID
        String correlationId = message.getMessageType() + "-" + System.currentTimeMillis();
        pendingResponses.put(correlationId, responseFuture);
        
        // Set correlation ID in message if supported
        message.setField(11, correlationId); // ClOrdID for orders
        
        sendMessage(message).whenComplete((result, throwable) -> {
            if (throwable != null) {
                pendingResponses.remove(correlationId);
                responseFuture.completeExceptionally(throwable);
            }
        });
        
        // Set timeout for response
        scheduledExecutor.schedule(() -> {
            CompletableFuture<FIXMessage> future = pendingResponses.remove(correlationId);
            if (future != null && !future.isDone()) {
                future.completeExceptionally(new FIXClientException("Response timeout"));
            }
        }, 30, TimeUnit.SECONDS);
        
        return responseFuture;
    }
    
    private void sendMessageInternal(FIXMessage message) throws Exception {
        BufferedWriter currentWriter = writer.get();
        if (currentWriter == null) {
            throw new FIXClientException("Not connected to server");
        }
        
        String fixString = message.toFixString();
        log.debug("Sending FIX message: {}", fixString.replace('\u0001', '|'));
        
        synchronized (currentWriter) {
            currentWriter.write(fixString);
            currentWriter.flush();
        }
    }
    
    private void messageReaderLoop() {
        log.info("Starting message reader loop");
        
        try {
            BufferedReader currentReader = reader.get();
            StringBuilder messageBuffer = new StringBuilder();
            
            while (connected.get() && currentReader != null) {
                int ch = currentReader.read();
                if (ch == -1) {
                    // End of stream
                    break;
                }
                
                messageBuffer.append((char) ch);
                
                // Check for complete message (ends with SOH after checksum field)
                if (ch == '\u0001') {
                    String currentMessage = messageBuffer.toString();
                    
                    // Check if this is a complete FIX message by looking for checksum field at the end
                    if (isCompleteFixMessage(currentMessage)) {
                        messageBuffer.setLength(0);
                        
                        try {
                            processIncomingMessage(currentMessage);
                        } catch (Exception e) {
                            log.error("Error processing incoming message: {}", currentMessage.replace('\u0001', '|'), e);
                        }
                    }
                    // If not complete, continue reading more data
                }
            }
        } catch (Exception e) {
            if (connected.get()) {
                log.error("Error in message reader loop", e);
                if (connectionHandler != null) {
                    connectionHandler.onError(this, e);
                }
            }
        } finally {
            log.info("Message reader loop ended");
            if (connected.get()) {
                try {
                    doDisconnect("Connection lost");
                } catch (Exception e) {
                    log.error("Error during disconnect after reader loop ended", e);
                }
            }
        }
    }
    
    private void processIncomingMessage(String messageString) throws Exception {
        log.debug("Received FIX message: {}", messageString.replace('\u0001', '|'));
        
        FIXMessage message = protocolHandler.parse(messageString);
        
        // Validate sequence number
        int expectedSeqNum = incomingSeqNum.get();
        if (message.getMessageSequenceNumber() != expectedSeqNum) {
            log.warn("Sequence number mismatch: expected {}, got {}", expectedSeqNum, message.getMessageSequenceNumber());
            // Handle sequence gap - could request resend
        } else {
            incomingSeqNum.incrementAndGet();
        }
        
        // Handle different message types
        String messageType = message.getMessageType();
        
        switch (messageType) {
            case "A": // Logon
                handleLogon(message);
                break;
            case "5": // Logout
                handleLogout(message);
                break;
            case "0": // Heartbeat
                handleHeartbeat(message);
                break;
            case "1": // Test Request
                handleTestRequest(message);
                break;
            default:
                // Check for pending responses
                String correlationId = message.getField(11); // ClOrdID
                if (correlationId != null) {
                    CompletableFuture<FIXMessage> future = pendingResponses.remove(correlationId);
                    if (future != null) {
                        future.complete(message);
                        return;
                    }
                }
                
                // Forward to message handler
                if (messageHandler != null) {
                    messageHandler.onMessage(message, this);
                }
                break;
        }
    }
    
    private void handleLogon(FIXMessage message) throws Exception {
        log.info("Received logon response");
        loggedOn.set(true);
        
        // Start heartbeat
        startHeartbeat();
        
        if (connectionHandler != null) {
            connectionHandler.onLoggedOn(this);
        }
    }
    
    private void handleLogout(FIXMessage message) throws Exception {
        String reason = message.getField(58); // Text
        log.info("Received logout message: {}", reason);
        
        loggedOn.set(false);
        
        if (connectionHandler != null) {
            connectionHandler.onLoggedOut(this);
        }
        
        doDisconnect("Server initiated logout: " + reason);
    }
    
    private void handleHeartbeat(FIXMessage message) {
        log.debug("Received heartbeat");
        lastHeartbeatTime = System.currentTimeMillis();
    }
    
    private void handleTestRequest(FIXMessage message) throws Exception {
        String testReqId = message.getField(112); // TestReqID
        log.debug("Received test request: {}", testReqId);
        
        // Send heartbeat response
        FIXMessage heartbeat = new FIXMessageImpl();
        heartbeat.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        heartbeat.setField(FIXMessage.MESSAGE_TYPE, MessageType.HEARTBEAT.getValue());
        heartbeat.setField(FIXMessage.SENDER_COMP_ID, config.getSenderCompId());
        heartbeat.setField(FIXMessage.TARGET_COMP_ID, config.getTargetCompId());
        heartbeat.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(outgoingSeqNum.getAndIncrement()));
        heartbeat.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        if (testReqId != null) {
            heartbeat.setField(112, testReqId); // TestReqID
        }
        
        sendMessageInternal(heartbeat);
    }
    
    private void startHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        
        lastHeartbeatTime = System.currentTimeMillis();
        
        heartbeatTask = scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                long timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime;
                long heartbeatIntervalMs = config.getHeartbeatInterval() * 1000L;
                
                if (timeSinceLastHeartbeat > heartbeatIntervalMs * 1.2) {
                    // Send test request
                    sendTestRequest();
                } else if (timeSinceLastHeartbeat > heartbeatIntervalMs) {
                    // Send heartbeat
                    sendHeartbeat();
                }
            } catch (Exception e) {
                log.error("Error in heartbeat task", e);
            }
        }, config.getHeartbeatInterval(), config.getHeartbeatInterval(), TimeUnit.SECONDS);
    }
    
    private void sendHeartbeat() throws Exception {
        FIXMessage heartbeat = new FIXMessageImpl();
        heartbeat.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        heartbeat.setField(FIXMessage.MESSAGE_TYPE, MessageType.HEARTBEAT.getValue());
        heartbeat.setField(FIXMessage.SENDER_COMP_ID, config.getSenderCompId());
        heartbeat.setField(FIXMessage.TARGET_COMP_ID, config.getTargetCompId());
        heartbeat.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(outgoingSeqNum.getAndIncrement()));
        heartbeat.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        sendMessageInternal(heartbeat);
        log.debug("Sent heartbeat");
    }
    
    private void sendTestRequest() throws Exception {
        String testReqId = "TEST_" + System.currentTimeMillis();
        
        FIXMessage testRequest = new FIXMessageImpl();
        testRequest.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        testRequest.setField(FIXMessage.MESSAGE_TYPE, MessageType.TEST_REQUEST.getValue());
        testRequest.setField(FIXMessage.SENDER_COMP_ID, config.getSenderCompId());
        testRequest.setField(FIXMessage.TARGET_COMP_ID, config.getTargetCompId());
        testRequest.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(outgoingSeqNum.getAndIncrement()));
        testRequest.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        testRequest.setField(112, testReqId); // TestReqID
        
        sendMessageInternal(testRequest);
        log.debug("Sent test request: {}", testReqId);
    }
    
    @Override
    public boolean isConnected() {
        return connected.get() && loggedOn.get();
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public void setMessageHandler(FIXClientMessageHandler handler) {
        this.messageHandler = handler;
    }
    
    @Override
    public void setConnectionHandler(FIXClientConnectionHandler handler) {
        this.connectionHandler = handler;
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
     * Shuts down the client and releases all resources.
     */
    public void shutdown() {
        try {
            disconnect().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
        
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}