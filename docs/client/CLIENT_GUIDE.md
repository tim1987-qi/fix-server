# FIX Client Integration Guide

## 📋 Overview

This guide provides comprehensive documentation for integrating applications with the FIX Server using the provided Java client library. The FIX client supports both traditional socket-based connections and high-performance Netty-based connections.

## 🚀 Quick Start

### 1. Add Dependency

Add the FIX client dependency to your project:

```xml
<dependency>
    <groupId>com.fixserver</groupId>
    <artifactId>fix-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Basic Client Setup

```java
import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;

public class BasicFIXClientExample {
    public static void main(String[] args) throws Exception {
        // Create client configuration
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9878)  // Traditional server port
            .senderCompId("CLIENT1")
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .build();
        
        // Create and configure client
        FIXClient client = FIXClientFactory.createClient(config);
        
        // Set connection handler
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                System.out.println("Connected to FIX server");
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("Disconnected from FIX server");
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                System.err.println("Connection error: " + error.getMessage());
            }
        });
        
        // Set message handler
        client.setMessageHandler(new FIXClientMessageHandler() {
            @Override
            public void onMessage(FIXClient client, FIXMessage message) {
                System.out.println("Received: " + message.toFixString());
            }
        });
        
        // Connect to server
        client.connect().get();
        
        // Send a market order
        FIXMessage order = OrderMessage.marketOrder("ORDER123", "AAPL", 100, OrderSide.BUY)
            .toFIXMessage();
        client.sendMessage(order).get();
        
        // Keep running for a while
        Thread.sleep(30000);
        
        // Cleanup
        client.disconnect().get();
        client.shutdown();
    }
}
```

## 🔧 Client Configuration

### Configuration Builder

The `FIXClientConfiguration` class provides a fluent builder interface for client setup:

```java
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .host("localhost")                    // FIX server hostname
    .port(9878)                          // FIX server port
    .senderCompId("CLIENT1")             // Client identifier
    .targetCompId("SERVER1")             // Server identifier
    .heartbeatInterval(30)               // Heartbeat interval in seconds
    .connectionTimeout(30000)            // Connection timeout in milliseconds
    .resetSequenceNumbers(false)         // Reset sequence numbers on logon
    .build();
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `host` | String | "localhost" | FIX server hostname or IP address |
| `port` | int | 9878 | FIX server port number |
| `senderCompId` | String | Required | Client company identifier |
| `targetCompId` | String | Required | Server company identifier |
| `heartbeatInterval` | int | 30 | Heartbeat interval in seconds |
| `connectionTimeout` | int | 30000 | Connection timeout in milliseconds |
| `resetSequenceNumbers` | boolean | false | Reset sequence numbers on logon |

### Default Configuration

For simple setups, use the default configuration:

```java
FIXClientConfiguration config = FIXClientConfiguration.defaultConfig("CLIENT1", "SERVER1");
FIXClient client = FIXClientFactory.createClient(config);
```

### Factory Methods

The `FIXClientFactory` provides convenient factory methods:

```java
// Using configuration object
FIXClient client = FIXClientFactory.createClient(config);

// Using basic parameters
FIXClient client = FIXClientFactory.createClient("localhost", 9878, "CLIENT1", "SERVER1");

// Using builder pattern
FIXClient client = FIXClientFactory.builder()
    .host("localhost")
    .port(9878)
    .senderCompId("CLIENT1")
    .targetCompId("SERVER1")
    .build()
    .createClient();
```

## 📡 Connection Management

### Connecting to Server

```java
// Asynchronous connection
CompletableFuture<Void> connectFuture = client.connect();

// Synchronous connection with timeout
try {
    client.connect().get(30, TimeUnit.SECONDS);
    System.out.println("Connected successfully");
} catch (TimeoutException e) {
    System.err.println("Connection timeout");
} catch (ExecutionException e) {
    System.err.println("Connection failed: " + e.getCause().getMessage());
}
```

### Connection State

```java
// Check connection status
if (client.isConnected()) {
    System.out.println("Client is connected");
} else {
    System.out.println("Client is not connected");
}
```

### Disconnecting

```java
// Graceful disconnect
CompletableFuture<Void> disconnectFuture = client.disconnect();

// Synchronous disconnect
try {
    client.disconnect().get(10, TimeUnit.SECONDS);
    System.out.println("Disconnected successfully");
} catch (Exception e) {
    System.err.println("Disconnect failed: " + e.getMessage());
}
```

### Connection Handlers

Implement `FIXClientConnectionHandler` to handle connection events:

```java
public class MyConnectionHandler implements FIXClientConnectionHandler {
    
    @Override
    public void onConnect(FIXClient client) {
        System.out.println("Connected to FIX server");
        // Initialize session-specific data
        // Start sending heartbeats
    }
    
    @Override
    public void onDisconnect(FIXClient client) {
        System.out.println("Disconnected from FIX server");
        // Clean up session data
        // Stop background tasks
    }
    
    @Override
    public void onError(FIXClient client, Throwable error) {
        System.err.println("Connection error: " + error.getMessage());
        // Log error details
        // Implement reconnection logic if needed
    }
}

// Register the handler
client.setConnectionHandler(new MyConnectionHandler());
```

## 📨 Message Handling

### Sending Messages

#### Basic Message Sending

```java
// Create a FIX message
FIXMessage message = new FIXMessageImpl("FIX.4.4", "D"); // New Order Single
message.setField(FIXTags.SYMBOL, "AAPL");
message.setField(FIXTags.SIDE, "1"); // Buy
message.setField(FIXTags.ORDER_QTY, "100");
message.setField(FIXTags.ORDER_TYPE, "1"); // Market

// Send asynchronously
CompletableFuture<Void> sendFuture = client.sendMessage(message);

// Send synchronously
try {
    client.sendMessage(message).get(5, TimeUnit.SECONDS);
    System.out.println("Message sent successfully");
} catch (Exception e) {
    System.err.println("Failed to send message: " + e.getMessage());
}
```

#### Using Order Message Helper

```java
// Market order
FIXMessage marketOrder = OrderMessage.marketOrder("ORDER123", "AAPL", 100, OrderSide.BUY)
    .toFIXMessage();
client.sendMessage(marketOrder);

// Limit order
FIXMessage limitOrder = OrderMessage.limitOrder("ORDER124", "GOOGL", 50, OrderSide.SELL, 2800.00)
    .toFIXMessage();
client.sendMessage(limitOrder);
```

#### Builder Pattern for Messages

```java
FIXMessage message = FIXMessage.builder()
    .version("FIX.4.4")
    .messageType("D") // New Order Single
    .field(FIXTags.SENDER_COMP_ID, "CLIENT1")
    .field(FIXTags.TARGET_COMP_ID, "SERVER1")
    .field(FIXTags.SYMBOL, "AAPL")
    .field(FIXTags.SIDE, "1") // Buy
    .field(FIXTags.ORDER_QTY, "100")
    .field(FIXTags.ORDER_TYPE, "2") // Limit
    .field(FIXTags.PRICE, "150.00")
    .build();

client.sendMessage(message);
```

### Receiving Messages

Implement `FIXClientMessageHandler` to handle incoming messages:

```java
public class MyMessageHandler implements FIXClientMessageHandler {
    
    @Override
    public void onMessage(FIXClient client, FIXMessage message) {
        String messageType = message.getMessageType();
        
        switch (messageType) {
            case "8": // Execution Report
                handleExecutionReport(message);
                break;
            case "9": // Order Cancel Reject
                handleOrderCancelReject(message);
                break;
            case "0": // Heartbeat
                handleHeartbeat(message);
                break;
            default:
                System.out.println("Received unknown message type: " + messageType);
        }
    }
    
    private void handleExecutionReport(FIXMessage message) {
        String orderId = message.getField(FIXTags.ORDER_ID);
        String execType = message.getField(FIXTags.EXEC_TYPE);
        String orderStatus = message.getField(FIXTags.ORDER_STATUS);
        
        System.out.printf("Execution Report - Order: %s, ExecType: %s, Status: %s%n",
                         orderId, execType, orderStatus);
    }
    
    private void handleOrderCancelReject(FIXMessage message) {
        String orderId = message.getField(FIXTags.ORDER_ID);
        String reason = message.getField(FIXTags.TEXT);
        
        System.out.printf("Order Cancel Reject - Order: %s, Reason: %s%n", orderId, reason);
    }
    
    private void handleHeartbeat(FIXMessage message) {
        // Heartbeat received - connection is alive
        System.out.println("Heartbeat received");
    }
}

// Register the handler
client.setMessageHandler(new MyMessageHandler());
```

## 🏪 Order Management

### Creating Orders

#### Market Orders

```java
// Buy market order
OrderMessage buyOrder = OrderMessage.marketOrder("BUY_ORDER_001", "AAPL", 100, OrderSide.BUY);
FIXMessage buyMessage = buyOrder.toFIXMessage();
client.sendMessage(buyMessage);

// Sell market order
OrderMessage sellOrder = OrderMessage.marketOrder("SELL_ORDER_001", "AAPL", 50, OrderSide.SELL);
FIXMessage sellMessage = sellOrder.toFIXMessage();
client.sendMessage(sellMessage);
```

#### Limit Orders

```java
// Buy limit order
OrderMessage buyLimitOrder = OrderMessage.limitOrder("LIMIT_BUY_001", "GOOGL", 10, OrderSide.BUY, 2750.00);
FIXMessage buyLimitMessage = buyLimitOrder.toFIXMessage();
client.sendMessage(buyLimitMessage);

// Sell limit order
OrderMessage sellLimitOrder = OrderMessage.limitOrder("LIMIT_SELL_001", "GOOGL", 5, OrderSide.SELL, 2850.00);
FIXMessage sellLimitMessage = sellLimitOrder.toFIXMessage();
client.sendMessage(sellLimitMessage);
```

#### Custom Orders

```java
FIXMessage customOrder = FIXMessage.builder()
    .version("FIX.4.4")
    .messageType("D") // New Order Single
    .field(FIXTags.CL_ORD_ID, "CUSTOM_ORDER_001")
    .field(FIXTags.SYMBOL, "TSLA")
    .field(FIXTags.SIDE, "1") // Buy
    .field(FIXTags.ORDER_QTY, "25")
    .field(FIXTags.ORDER_TYPE, "3") // Stop
    .field(FIXTags.STOP_PX, "800.00")
    .field(FIXTags.TIME_IN_FORCE, "0") // Day
    .build();

client.sendMessage(customOrder);
```

### Order Modifications

#### Cancel Order

```java
FIXMessage cancelRequest = FIXMessage.builder()
    .version("FIX.4.4")
    .messageType("F") // Order Cancel Request
    .field(FIXTags.ORIG_CL_ORD_ID, "ORIGINAL_ORDER_ID")
    .field(FIXTags.CL_ORD_ID, "CANCEL_REQUEST_001")
    .field(FIXTags.SYMBOL, "AAPL")
    .field(FIXTags.SIDE, "1")
    .build();

client.sendMessage(cancelRequest);
```

#### Modify Order

```java
FIXMessage modifyRequest = FIXMessage.builder()
    .version("FIX.4.4")
    .messageType("G") // Order Cancel/Replace Request
    .field(FIXTags.ORIG_CL_ORD_ID, "ORIGINAL_ORDER_ID")
    .field(FIXTags.CL_ORD_ID, "MODIFY_REQUEST_001")
    .field(FIXTags.SYMBOL, "AAPL")
    .field(FIXTags.SIDE, "1")
    .field(FIXTags.ORDER_QTY, "200") // New quantity
    .field(FIXTags.PRICE, "155.00") // New price
    .build();

client.sendMessage(modifyRequest);
```

## 🔄 Session Management

### Session Lifecycle

```java
public class SessionLifecycleHandler implements FIXClientConnectionHandler, FIXClientMessageHandler {
    
    private final AtomicBoolean loggedOn = new AtomicBoolean(false);
    private final Map<String, String> sessionData = new ConcurrentHashMap<>();
    
    @Override
    public void onConnect(FIXClient client) {
        System.out.println("TCP connection established");
        // FIX logon will be sent automatically by the client
    }
    
    @Override
    public void onMessage(FIXClient client, FIXMessage message) {
        String messageType = message.getMessageType();
        
        if ("A".equals(messageType)) { // Logon
            handleLogon(message);
        } else if ("5".equals(messageType)) { // Logout
            handleLogout(message);
        } else if (loggedOn.get()) {
            // Process business messages only when logged on
            processBusinessMessage(message);
        }
    }
    
    private void handleLogon(FIXMessage message) {
        loggedOn.set(true);
        System.out.println("FIX session logged on");
        
        // Store session parameters
        sessionData.put("heartbeatInterval", message.getField(FIXTags.HEARTBEAT_INTERVAL));
        
        // Start business logic
        startBusinessLogic();
    }
    
    private void handleLogout(FIXMessage message) {
        loggedOn.set(false);
        System.out.println("FIX session logged out");
        
        // Clean up session data
        sessionData.clear();
        
        // Stop business logic
        stopBusinessLogic();
    }
    
    private void processBusinessMessage(FIXMessage message) {
        // Process business messages here
    }
    
    private void startBusinessLogic() {
        // Initialize business logic
    }
    
    private void stopBusinessLogic() {
        // Clean up business logic
    }
    
    @Override
    public void onDisconnect(FIXClient client) {
        loggedOn.set(false);
        System.out.println("TCP connection closed");
    }
    
    @Override
    public void onError(FIXClient client, Throwable error) {
        System.err.println("Session error: " + error.getMessage());
        loggedOn.set(false);
    }
}
```

### Sequence Number Management

The client automatically handles sequence number management, but you can monitor and control it:

```java
public class SequenceNumberMonitor implements FIXClientMessageHandler {
    
    private int expectedInSeqNum = 1;
    private int nextOutSeqNum = 1;
    
    @Override
    public void onMessage(FIXClient client, FIXMessage message) {
        // Check incoming sequence number
        String seqNumStr = message.getField(FIXTags.MSG_SEQ_NUM);
        if (seqNumStr != null) {
            int seqNum = Integer.parseInt(seqNumStr);
            
            if (seqNum == expectedInSeqNum) {
                expectedInSeqNum++;
                processMessage(message);
            } else if (seqNum > expectedInSeqNum) {
                // Gap detected - request resend
                requestResend(client, expectedInSeqNum, seqNum - 1);
            } else {
                // Duplicate or out-of-order message
                System.out.println("Received duplicate/out-of-order message: " + seqNum);
            }
        }
    }
    
    private void requestResend(FIXClient client, int beginSeqNum, int endSeqNum) {
        FIXMessage resendRequest = FIXMessage.builder()
            .version("FIX.4.4")
            .messageType("2") // Resend Request
            .field(FIXTags.BEGIN_SEQ_NO, String.valueOf(beginSeqNum))
            .field(FIXTags.END_SEQ_NO, String.valueOf(endSeqNum))
            .build();
        
        try {
            client.sendMessage(resendRequest).get();
            System.out.printf("Requested resend for messages %d-%d%n", beginSeqNum, endSeqNum);
        } catch (Exception e) {
            System.err.println("Failed to send resend request: " + e.getMessage());
        }
    }
    
    private void processMessage(FIXMessage message) {
        // Process the message
        System.out.println("Processing message: " + message.getMessageType());
    }
}
```

## 🚀 High-Performance Client

### Netty Client Configuration

For high-performance scenarios, connect to the Netty server:

```java
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .host("localhost")
    .port(9879)  // Netty server port
    .senderCompId("HIGH_PERF_CLIENT")
    .targetCompId("SERVER1")
    .heartbeatInterval(30)
    .build();

FIXClient client = FIXClientFactory.createClient(config);
```

### Optimized Message Processing

```java
public class HighPerformanceMessageHandler implements FIXClientMessageHandler {
    
    // Pre-allocate objects to reduce GC pressure
    private final StringBuilder stringBuilder = new StringBuilder(1024);
    private final Map<String, String> fieldCache = new HashMap<>();
    
    @Override
    public void onMessage(FIXClient client, FIXMessage message) {
        // Use object pooling and caching for high-frequency processing
        processMessageOptimized(message);
    }
    
    private void processMessageOptimized(FIXMessage message) {
        // Clear reusable objects
        stringBuilder.setLength(0);
        fieldCache.clear();
        
        // Cache frequently accessed fields
        fieldCache.put("messageType", message.getMessageType());
        fieldCache.put("symbol", message.getField(FIXTags.SYMBOL));
        fieldCache.put("side", message.getField(FIXTags.SIDE));
        
        // Process based on message type
        String messageType = fieldCache.get("messageType");
        switch (messageType) {
            case "8": // Execution Report
                processExecutionReportFast(fieldCache);
                break;
            case "D": // New Order Single
                processNewOrderFast(fieldCache);
                break;
            default:
                // Handle other message types
                break;
        }
    }
    
    private void processExecutionReportFast(Map<String, String> fields) {
        // High-performance execution report processing
        // Minimize object allocation and string operations
    }
    
    private void processNewOrderFast(Map<String, String> fields) {
        // High-performance new order processing
    }
}
```

## 🔒 Error Handling

### Connection Error Handling

```java
public class RobustConnectionHandler implements FIXClientConnectionHandler {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final int maxReconnectAttempts = 5;
    private final int reconnectDelaySeconds = 10;
    
    @Override
    public void onConnect(FIXClient client) {
        System.out.println("Connected successfully");
        reconnectAttempts.set(0); // Reset counter on successful connection
    }
    
    @Override
    public void onDisconnect(FIXClient client) {
        System.out.println("Disconnected from server");
        attemptReconnect(client);
    }
    
    @Override
    public void onError(FIXClient client, Throwable error) {
        System.err.println("Connection error: " + error.getMessage());
        
        if (error instanceof FIXClientException) {
            FIXClientException fixError = (FIXClientException) error;
            handleFIXError(client, fixError);
        } else {
            // Handle other types of errors
            attemptReconnect(client);
        }
    }
    
    private void handleFIXError(FIXClient client, FIXClientException error) {
        // Handle FIX-specific errors
        System.err.println("FIX error: " + error.getMessage());
        
        // Decide whether to reconnect based on error type
        if (isRecoverableError(error)) {
            attemptReconnect(client);
        } else {
            System.err.println("Non-recoverable FIX error, stopping client");
        }
    }
    
    private boolean isRecoverableError(FIXClientException error) {
        // Determine if the error is recoverable
        String message = error.getMessage().toLowerCase();
        return message.contains("connection") || 
               message.contains("timeout") || 
               message.contains("network");
    }
    
    private void attemptReconnect(FIXClient client) {
        int attempts = reconnectAttempts.incrementAndGet();
        
        if (attempts <= maxReconnectAttempts) {
            System.out.printf("Attempting reconnect %d/%d in %d seconds%n", 
                             attempts, maxReconnectAttempts, reconnectDelaySeconds);
            
            scheduler.schedule(() -> {
                try {
                    client.connect().get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.err.println("Reconnect attempt failed: " + e.getMessage());
                    attemptReconnect(client); // Recursive retry
                }
            }, reconnectDelaySeconds, TimeUnit.SECONDS);
        } else {
            System.err.println("Max reconnect attempts reached, giving up");
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}
```

### Message Error Handling

```java
public class ErrorHandlingMessageHandler implements FIXClientMessageHandler {
    
    @Override
    public void onMessage(FIXClient client, FIXMessage message) {
        try {
            processMessage(message);
        } catch (Exception e) {
            handleMessageError(client, message, e);
        }
    }
    
    private void processMessage(FIXMessage message) throws Exception {
        String messageType = message.getMessageType();
        
        // Validate required fields
        validateMessage(message);
        
        // Process based on type
        switch (messageType) {
            case "8": // Execution Report
                processExecutionReport(message);
                break;
            case "9": // Order Cancel Reject
                processOrderCancelReject(message);
                break;
            default:
                throw new IllegalArgumentException("Unknown message type: " + messageType);
        }
    }
    
    private void validateMessage(FIXMessage message) throws Exception {
        // Validate required fields based on message type
        String messageType = message.getMessageType();
        
        if ("8".equals(messageType)) { // Execution Report
            if (!message.hasField(FIXTags.ORDER_ID)) {
                throw new Exception("Missing required field: OrderID");
            }
            if (!message.hasField(FIXTags.EXEC_TYPE)) {
                throw new Exception("Missing required field: ExecType");
            }
        }
    }
    
    private void handleMessageError(FIXClient client, FIXMessage message, Exception error) {
        System.err.printf("Error processing message %s: %s%n", 
                         message.getMessageType(), error.getMessage());
        
        // Log the problematic message
        System.err.println("Problematic message: " + message.toFixString());
        
        // Send reject if appropriate
        if (shouldSendReject(error)) {
            sendBusinessReject(client, message, error.getMessage());
        }
    }
    
    private boolean shouldSendReject(Exception error) {
        // Determine if we should send a business reject
        return error instanceof IllegalArgumentException || 
               error.getMessage().contains("Missing required field");
    }
    
    private void sendBusinessReject(FIXClient client, FIXMessage originalMessage, String reason) {
        try {
            FIXMessage reject = FIXMessage.builder()
                .version("FIX.4.4")
                .messageType("j") // Business Message Reject
                .field(FIXTags.REF_MSG_TYPE, originalMessage.getMessageType())
                .field(FIXTags.BUSINESS_REJECT_REASON, "1") // Other
                .field(FIXTags.TEXT, reason)
                .build();
            
            client.sendMessage(reject);
        } catch (Exception e) {
            System.err.println("Failed to send business reject: " + e.getMessage());
        }
    }
    
    private void processExecutionReport(FIXMessage message) {
        // Process execution report
    }
    
    private void processOrderCancelReject(FIXMessage message) {
        // Process order cancel reject
    }
}
```

## 📊 Monitoring and Logging

### Client Metrics

```java
public class MetricsCollectingClient {
    
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    public void setupClient() {
        FIXClient client = FIXClientFactory.createClient(config);
        
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                connectionCount.incrementAndGet();
                System.out.println("Connected. Total connections: " + connectionCount.get());
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("Disconnected");
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                errorCount.incrementAndGet();
                System.err.println("Error count: " + errorCount.get());
            }
        });
        
        client.setMessageHandler(new FIXClientMessageHandler() {
            @Override
            public void onMessage(FIXClient client, FIXMessage message) {
                messagesReceived.incrementAndGet();
                
                if (messagesReceived.get() % 1000 == 0) {
                    System.out.printf("Messages received: %d, sent: %d%n", 
                                     messagesReceived.get(), messagesSent.get());
                }
            }
        });
    }
    
    public void sendMessage(FIXClient client, FIXMessage message) {
        try {
            client.sendMessage(message).get();
            messagesSent.incrementAndGet();
        } catch (Exception e) {
            errorCount.incrementAndGet();
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }
    
    public void printStatistics() {
        System.out.printf("Statistics - Sent: %d, Received: %d, Connections: %d, Errors: %d%n",
                         messagesSent.get(), messagesReceived.get(), 
                         connectionCount.get(), errorCount.get());
    }
}
```

## 🧪 Testing Client Integration

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class FIXClientTest {
    
    @Mock
    private FIXClientConnectionHandler connectionHandler;
    
    @Mock
    private FIXClientMessageHandler messageHandler;
    
    private FIXClient client;
    
    @BeforeEach
    void setUp() {
        FIXClientConfiguration config = FIXClientConfiguration.defaultConfig("CLIENT1", "SERVER1");
        client = FIXClientFactory.createClient(config);
        client.setConnectionHandler(connectionHandler);
        client.setMessageHandler(messageHandler);
    }
    
    @Test
    void shouldHandleConnectionEvents() {
        // Test connection handling
        verify(connectionHandler, never()).onConnect(any());
        
        // Simulate connection
        // ... test implementation
    }
    
    @Test
    void shouldSendMessagesCorrectly() {
        // Test message sending
        FIXMessage message = OrderMessage.marketOrder("TEST", "AAPL", 100, OrderSide.BUY)
            .toFIXMessage();
        
        assertDoesNotThrow(() -> {
            client.sendMessage(message);
        });
    }
}
```

### Integration Testing

```java
@SpringBootTest
@ActiveProfiles("test")
class FIXClientIntegrationTest {
    
    @LocalServerPort
    private int serverPort;
    
    @Test
    void shouldConnectAndExchangeMessages() throws Exception {
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(serverPort)
            .senderCompId("TEST_CLIENT")
            .targetCompId("SERVER1")
            .build();
        
        FIXClient client = FIXClientFactory.createClient(config);
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<FIXMessage> receivedMessage = new AtomicReference<>();
        
        client.setMessageHandler((c, msg) -> {
            receivedMessage.set(msg);
            messageLatch.countDown();
        });
        
        // Connect and send message
        client.connect().get(5, TimeUnit.SECONDS);
        
        FIXMessage order = OrderMessage.marketOrder("TEST_ORDER", "AAPL", 100, OrderSide.BUY)
            .toFIXMessage();
        client.sendMessage(order).get(1, TimeUnit.SECONDS);
        
        // Wait for response
        assertThat(messageLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessage.get()).isNotNull();
        
        client.disconnect().get(1, TimeUnit.SECONDS);
    }
}
```

## 📚 Best Practices

### 1. Connection Management
- Always use connection handlers to monitor connection state
- Implement reconnection logic for production systems
- Set appropriate timeouts for connection operations

### 2. Message Processing
- Use asynchronous message processing for high-throughput scenarios
- Implement proper error handling for message validation
- Cache frequently accessed message fields

### 3. Resource Management
- Always call `shutdown()` when done with the client
- Use try-with-resources or finally blocks for cleanup
- Monitor memory usage in high-frequency scenarios

### 4. Performance Optimization
- Use the Netty server port (9879) for high-performance scenarios
- Minimize object allocation in message handlers
- Consider object pooling for high-frequency trading

### 5. Error Handling
- Implement comprehensive error handling for all scenarios
- Log errors with sufficient detail for debugging
- Use circuit breaker patterns for resilient systems

## 📚 Additional Resources

- **[Examples](EXAMPLES.md)** - More detailed usage examples
- **[API Reference](../development/API_REFERENCE.md)** - Complete API documentation
- **[Performance Guide](../performance/PERFORMANCE_GUIDE.md)** - Performance optimization tips
- **[FIX Protocol Specification](https://www.fixtrading.org/standards/)** - Official FIX protocol documentation