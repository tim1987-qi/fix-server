# FIX Server API Reference

## 📋 Overview

This document provides a comprehensive reference for all APIs available in the FIX Server, including REST endpoints for management, Java APIs for client integration, and FIX protocol message specifications.

## 🌐 REST Management API

### Base URL
- **Development**: `http://localhost:8080`
- **Production**: `https://your-domain.com`

### Authentication
Management endpoints require basic authentication:
```
Username: admin
Password: admin123 (change in production!)
```

### Health and Status Endpoints

#### GET /actuator/health
Returns the overall health status of the FIX server.

**Response:**
```json
{
  "status": "UP",
  "components": {
    "fixServer": {
      "status": "UP",
      "details": {
        "port": 9878,
        "activeSessions": 5,
        "totalConnections": 127
      }
    },
    "nettyServer": {
      "status": "UP",
      "details": {
        "port": 9879,
        "activeConnections": 23,
        "workerThreads": 8
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

#### GET /actuator/info
Returns application information and version details.

**Response:**
```json
{
  "app": {
    "name": "FIX Server",
    "description": "Enterprise-grade FIX protocol server for financial trading",
    "version": "1.0.0-SNAPSHOT"
  },
  "build": {
    "version": "1.0.0-SNAPSHOT",
    "artifact": "fix-server",
    "name": "fix-server",
    "group": "com.fixserver",
    "time": "2025-01-01T12:00:00.000Z"
  }
}
```

### Metrics Endpoints

#### GET /actuator/metrics
Lists all available metrics.

**Response:**
```json
{
  "names": [
    "fix.server.messages.received",
    "fix.server.messages.sent",
    "fix.server.sessions.active",
    "fix.server.connections.active",
    "fix.server.message.processing.time",
    "jvm.memory.used",
    "jvm.gc.pause"
  ]
}
```

#### GET /actuator/metrics/{metricName}
Returns detailed information about a specific metric.

**Example: GET /actuator/metrics/fix.server.message.processing.time**

**Response:**
```json
{
  "name": "fix.server.message.processing.time",
  "description": "Time taken to process FIX messages",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1247.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 0.074321
    },
    {
      "statistic": "MAX",
      "value": 0.000156
    }
  ],
  "availableTags": [
    {
      "tag": "message.type",
      "values": ["D", "8", "A", "0"]
    }
  ]
}
```

#### GET /actuator/prometheus
Returns metrics in Prometheus format for monitoring integration.

**Response:**
```
# HELP fix_server_messages_received_total Total number of FIX messages received
# TYPE fix_server_messages_received_total counter
fix_server_messages_received_total{message_type="D"} 456.0
fix_server_messages_received_total{message_type="8"} 123.0

# HELP fix_server_message_processing_time_seconds Time taken to process FIX messages
# TYPE fix_server_message_processing_time_seconds summary
fix_server_message_processing_time_seconds_count 1247.0
fix_server_message_processing_time_seconds_sum 0.074321
fix_server_message_processing_time_seconds{quantile="0.5"} 0.000059
fix_server_message_processing_time_seconds{quantile="0.95"} 0.000098
fix_server_message_processing_time_seconds{quantile="0.99"} 0.000156
```

## 🔌 Java Client API

### FIX Client Interface

#### Creating a FIX Client

```java
// Using factory with configuration
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .host("localhost")
    .port(9878)
    .senderCompId("CLIENT1")
    .targetCompId("SERVER1")
    .heartbeatInterval(30)
    .build();

FIXClient client = FIXClientFactory.createClient(config);

// Using factory with basic parameters
FIXClient client = FIXClientFactory.createClient("localhost", 9878, "CLIENT1", "SERVER1");

// Using default configuration
FIXClientConfiguration config = FIXClientConfiguration.defaultConfig("CLIENT1", "SERVER1");
FIXClient client = FIXClientFactory.createClient(config);
```

#### FIXClient Interface Methods

```java
public interface FIXClient {
    /**
     * Connect to the FIX server
     * @return CompletableFuture that completes when connection is established
     */
    CompletableFuture<Void> connect();
    
    /**
     * Disconnect from the FIX server
     * @return CompletableFuture that completes when disconnection is complete
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Send a FIX message to the server
     * @param message the FIX message to send
     * @return CompletableFuture that completes when message is sent
     */
    CompletableFuture<Void> sendMessage(FIXMessage message);
    
    /**
     * Check if client is connected
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Register a connection handler
     * @param handler the connection handler
     */
    void setConnectionHandler(FIXClientConnectionHandler handler);
    
    /**
     * Register a message handler
     * @param handler the message handler
     */
    void setMessageHandler(FIXClientMessageHandler handler);
    
    /**
     * Shutdown the client and release resources
     */
    void shutdown();
}
```

#### Connection Handler

```java
public interface FIXClientConnectionHandler {
    /**
     * Called when client connects to server
     * @param client the FIX client
     */
    void onConnect(FIXClient client);
    
    /**
     * Called when client disconnects from server
     * @param client the FIX client
     */
    void onDisconnect(FIXClient client);
    
    /**
     * Called when connection error occurs
     * @param client the FIX client
     * @param error the error that occurred
     */
    void onError(FIXClient client, Throwable error);
}
```

#### Message Handler

```java
public interface FIXClientMessageHandler {
    /**
     * Called when a FIX message is received
     * @param client the FIX client
     * @param message the received FIX message
     */
    void onMessage(FIXClient client, FIXMessage message);
}
```

### FIX Message API

#### FIXMessage Interface

```java
public interface FIXMessage {
    /**
     * Get the FIX version (e.g., "FIX.4.4")
     * @return the FIX version
     */
    String getVersion();
    
    /**
     * Get the message type (e.g., "D" for New Order Single)
     * @return the message type
     */
    String getMessageType();
    
    /**
     * Get a field value by tag
     * @param tag the FIX tag
     * @return the field value, or null if not present
     */
    String getField(int tag);
    
    /**
     * Set a field value
     * @param tag the FIX tag
     * @param value the field value
     */
    void setField(int tag, String value);
    
    /**
     * Check if a field is present
     * @param tag the FIX tag
     * @return true if field is present, false otherwise
     */
    boolean hasField(int tag);
    
    /**
     * Remove a field
     * @param tag the FIX tag
     */
    void removeField(int tag);
    
    /**
     * Get all field tags
     * @return set of all field tags
     */
    Set<Integer> getFieldTags();
    
    /**
     * Convert message to FIX string format
     * @return the FIX string representation
     */
    String toFixString();
    
    /**
     * Create a builder for this message type
     * @return message builder
     */
    static FIXMessageBuilder builder() {
        return new FIXMessageBuilder();
    }
}
```

#### Creating FIX Messages

```java
// Using builder pattern
FIXMessage message = FIXMessage.builder()
    .version("FIX.4.4")
    .messageType("D")  // New Order Single
    .field(FIXTags.SENDER_COMP_ID, "CLIENT1")
    .field(FIXTags.TARGET_COMP_ID, "SERVER1")
    .field(FIXTags.SYMBOL, "AAPL")
    .field(FIXTags.SIDE, "1")  // Buy
    .field(FIXTags.ORDER_QTY, "100")
    .field(FIXTags.ORDER_TYPE, "2")  // Limit
    .field(FIXTags.PRICE, "150.00")
    .build();

// Using constructor
FIXMessage message = new FIXMessageImpl("FIX.4.4", "D");
message.setField(FIXTags.SYMBOL, "AAPL");
message.setField(FIXTags.SIDE, "1");
message.setField(FIXTags.ORDER_QTY, "100");

// Using order message helper
OrderMessage orderMessage = OrderMessage.marketOrder("ORDER123", "AAPL", 100, OrderSide.BUY);
FIXMessage message = orderMessage.toFIXMessage();
```

### Order Message Helper API

```java
public class OrderMessage {
    /**
     * Create a market order
     * @param orderId the order ID
     * @param symbol the symbol
     * @param quantity the quantity
     * @param side the order side
     * @return OrderMessage instance
     */
    public static OrderMessage marketOrder(String orderId, String symbol, int quantity, OrderSide side);
    
    /**
     * Create a limit order
     * @param orderId the order ID
     * @param symbol the symbol
     * @param quantity the quantity
     * @param side the order side
     * @param price the limit price
     * @return OrderMessage instance
     */
    public static OrderMessage limitOrder(String orderId, String symbol, int quantity, OrderSide side, double price);
    
    /**
     * Convert to FIX message
     * @return FIX message representation
     */
    public FIXMessage toFIXMessage();
}

public enum OrderSide {
    BUY("1"),
    SELL("2");
    
    private final String fixValue;
    
    OrderSide(String fixValue) {
        this.fixValue = fixValue;
    }
    
    public String getFixValue() {
        return fixValue;
    }
}
```

## 📨 FIX Protocol Messages

### Supported Message Types

| Message Type | Name | Description |
|--------------|------|-------------|
| A | Logon | Session logon message |
| 0 | Heartbeat | Heartbeat message |
| 1 | Test Request | Test request message |
| 2 | Resend Request | Request for message resend |
| 4 | Sequence Reset | Reset sequence numbers |
| 5 | Logout | Session logout message |
| 8 | Execution Report | Order execution report |
| 9 | Order Cancel Reject | Order cancel rejection |
| D | New Order Single | New single order |
| F | Order Cancel Request | Cancel order request |
| G | Order Cancel/Replace Request | Modify order request |

### Common FIX Tags

```java
public class FIXTags {
    // Standard Header Tags
    public static final int BEGIN_STRING = 8;
    public static final int BODY_LENGTH = 9;
    public static final int MSG_TYPE = 35;
    public static final int SENDER_COMP_ID = 49;
    public static final int TARGET_COMP_ID = 56;
    public static final int MSG_SEQ_NUM = 34;
    public static final int SENDING_TIME = 52;
    
    // Session Tags
    public static final int HEARTBEAT_INTERVAL = 108;
    public static final int TEST_REQ_ID = 112;
    public static final int RESET_SEQ_NUM_FLAG = 141;
    
    // Order Tags
    public static final int SYMBOL = 55;
    public static final int SIDE = 54;
    public static final int ORDER_QTY = 38;
    public static final int ORDER_TYPE = 40;
    public static final int PRICE = 44;
    public static final int ORDER_ID = 37;
    public static final int EXEC_ID = 17;
    public static final int EXEC_TYPE = 150;
    public static final int ORDER_STATUS = 39;
    
    // Standard Trailer Tags
    public static final int CHECKSUM = 10;
}
```

### Message Examples

#### Logon Message
```
8=FIX.4.4|9=65|35=A|34=1|49=CLIENT1|52=20250101-10:30:00|56=SERVER1|98=0|108=30|10=123|
```

#### New Order Single
```
8=FIX.4.4|9=154|35=D|34=2|49=CLIENT1|52=20250101-10:30:01|56=SERVER1|11=ORDER123|21=1|38=100|40=2|44=150.00|54=1|55=AAPL|59=0|10=456|
```

#### Execution Report
```
8=FIX.4.4|9=165|35=8|34=2|49=SERVER1|52=20250101-10:30:02|56=CLIENT1|6=150.00|11=ORDER123|14=100|17=EXEC123|20=0|37=ORDER123|38=100|39=2|54=1|55=AAPL|150=F|151=0|10=789|
```

## 🔧 Configuration API

### FIXClientConfiguration

```java
public class FIXClientConfiguration {
    private String host = "localhost";
    private int port = 9878;
    private String senderCompId;
    private String targetCompId;
    private int heartbeatInterval = 30;
    private int connectionTimeout = 30000;
    private boolean resetSequenceNumbers = false;
    
    // Builder pattern
    public static FIXClientConfigurationBuilder builder() {
        return new FIXClientConfigurationBuilder();
    }
    
    // Default configuration
    public static FIXClientConfiguration defaultConfig(String senderCompId, String targetCompId) {
        return builder()
            .senderCompId(senderCompId)
            .targetCompId(targetCompId)
            .build();
    }
    
    // Getters and setters...
}
```

### Configuration Builder

```java
public class FIXClientConfigurationBuilder {
    public FIXClientConfigurationBuilder host(String host);
    public FIXClientConfigurationBuilder port(int port);
    public FIXClientConfigurationBuilder senderCompId(String senderCompId);
    public FIXClientConfigurationBuilder targetCompId(String targetCompId);
    public FIXClientConfigurationBuilder heartbeatInterval(int heartbeatInterval);
    public FIXClientConfigurationBuilder connectionTimeout(int connectionTimeout);
    public FIXClientConfigurationBuilder resetSequenceNumbers(boolean resetSequenceNumbers);
    public FIXClientConfiguration build();
}
```

## 🚨 Exception Handling

### FIXClientException

```java
public class FIXClientException extends Exception {
    public FIXClientException(String message);
    public FIXClientException(String message, Throwable cause);
    public FIXClientException(Throwable cause);
}
```

### Common Exception Scenarios

```java
try {
    FIXClient client = FIXClientFactory.createClient(config);
    client.connect().get(30, TimeUnit.SECONDS);
    
    FIXMessage message = OrderMessage.marketOrder("ORDER123", "AAPL", 100, OrderSide.BUY)
        .toFIXMessage();
    client.sendMessage(message).get();
    
} catch (TimeoutException e) {
    // Connection timeout
    log.error("Connection timeout: {}", e.getMessage());
} catch (ExecutionException e) {
    // Execution error
    if (e.getCause() instanceof FIXClientException) {
        FIXClientException fixException = (FIXClientException) e.getCause();
        log.error("FIX client error: {}", fixException.getMessage());
    }
} catch (InterruptedException e) {
    // Thread interrupted
    Thread.currentThread().interrupt();
    log.warn("Operation interrupted");
}
```

## 📊 Monitoring API

### Custom Metrics

The server exposes custom metrics for monitoring:

```java
// Message processing metrics
fix.server.messages.received.total
fix.server.messages.sent.total
fix.server.message.processing.time

// Session metrics
fix.server.sessions.active
fix.server.sessions.created.total
fix.server.sessions.destroyed.total

// Connection metrics
fix.server.connections.active
fix.server.connections.total

// Performance metrics
fix.server.parser.time
fix.server.storage.time
fix.server.network.time
```

### Health Check API

Custom health indicators provide detailed status:

```java
// FIX Server health
GET /actuator/health/fixServer

// Netty Server health
GET /actuator/health/nettyServer

// Database health (if configured)
GET /actuator/health/db
```

## 📚 Usage Examples

### Complete Client Example

```java
public class FIXClientExample {
    public static void main(String[] args) throws Exception {
        // Create configuration
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9878)
            .senderCompId("CLIENT1")
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .build();
        
        // Create client
        FIXClient client = FIXClientFactory.createClient(config);
        
        // Set handlers
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
        
        client.setMessageHandler(new FIXClientMessageHandler() {
            @Override
            public void onMessage(FIXClient client, FIXMessage message) {
                System.out.println("Received: " + message.toFixString());
            }
        });
        
        // Connect and send message
        client.connect().get();
        
        FIXMessage order = OrderMessage.marketOrder("ORDER123", "AAPL", 100, OrderSide.BUY)
            .toFIXMessage();
        client.sendMessage(order).get();
        
        // Keep running
        Thread.sleep(60000);
        
        // Cleanup
        client.disconnect().get();
        client.shutdown();
    }
}
```

## 🔗 Additional Resources

- **[Client Guide](../client/CLIENT_GUIDE.md)** - Detailed client integration guide
- **[Examples](../client/EXAMPLES.md)** - More usage examples
- **[Development Guide](DEVELOPMENT_GUIDE.md)** - Development practices
- **[Architecture Guide](ARCHITECTURE.md)** - System architecture details