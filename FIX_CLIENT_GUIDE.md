# FIX Client Guide

This guide explains how to use the FIX client implementation to connect to FIX servers and send/receive messages.

## Overview

The FIX client provides a complete implementation of the FIX (Financial Information eXchange) protocol for connecting to FIX servers. It supports:

- **Connection Management**: Automatic connection, reconnection, and graceful disconnection
- **Session Management**: Logon/logout sequences with proper authentication
- **Message Handling**: Send and receive FIX messages with automatic sequence number management
- **Heartbeat Management**: Automatic heartbeat sending and monitoring
- **Error Recovery**: Sequence gap detection and resend request handling
- **Asynchronous Operations**: Non-blocking message sending and receiving

## Quick Start

### 1. Basic Client Usage

```java
// Create client configuration
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .host("localhost")
    .port(9876)
    .senderCompId("CLIENT1")
    .targetCompId("SERVER1")
    .heartbeatInterval(30)
    .build();

// Create client
FIXClient client = FIXClientFactory.createClient(config);

// Set up handlers
client.setConnectionHandler(new MyConnectionHandler());
client.setMessageHandler(new MyMessageHandler());

// Connect and send messages
client.connect().get();
client.sendMessage(myOrder).get();
client.disconnect().get();
```

### 2. Running the Example Client

The project includes an interactive example client that you can run:

**Linux/macOS:**
```bash
./run-client.sh [host] [port] [senderCompId] [targetCompId]
```

**Windows:**
```cmd
run-client.bat [host] [port] [senderCompId] [targetCompId]
```

**Default values:**
- Host: localhost
- Port: 9876
- Sender CompID: CLIENT1
- Target CompID: SERVER1

### 3. Interactive Commands

Once the client is running, you can use these commands:

- `market AAPL buy 100` - Send market order to buy 100 shares of AAPL
- `limit AAPL sell 50 150.00` - Send limit order to sell 50 shares at $150.00
- `cancel ORDER_1 AAPL buy` - Cancel a previous order
- `status ORDER_1 AAPL buy` - Get status of an order
- `quit` - Exit the client

## Configuration Options

### Basic Configuration

```java
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .host("fix.server.com")           // Server hostname
    .port(9876)                       // Server port
    .senderCompId("MY_CLIENT")        // Your company ID
    .targetCompId("EXCHANGE")         // Target company ID
    .fixVersion("FIX.4.4")           // FIX version (default: FIX.4.4)
    .heartbeatInterval(30)            // Heartbeat interval in seconds
    .build();
```

### Advanced Configuration

```java
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .host("fix.server.com")
    .port(9876)
    .senderCompId("MY_CLIENT")
    .targetCompId("EXCHANGE")
    
    // Timeouts
    .connectionTimeout(Duration.ofSeconds(30))
    .logonTimeout(Duration.ofSeconds(30))
    
    // Session options
    .resetSeqNumFlag(false)           // Reset sequence numbers on logon
    .validateMessages(true)           // Enable message validation
    .autoHeartbeat(true)             // Automatic heartbeat responses
    .autoResendRequest(true)         // Automatic resend requests
    
    // Reconnection
    .maxReconnectAttempts(3)         // Max reconnection attempts
    .reconnectDelay(Duration.ofSeconds(5))
    
    // Authentication (if required)
    .username("myuser")
    .password("mypass")
    
    // TLS/SSL (if required)
    .tlsEnabled(true)
    .truststorePath("/path/to/truststore.jks")
    .truststorePassword("trustpass")
    .keystorePath("/path/to/keystore.jks")
    .keystorePassword("keypass")
    
    .build();
```

## Message Handling

### Connection Events

```java
client.setConnectionHandler(new FIXClientConnectionHandler() {
    @Override
    public void onConnected(FIXClient client) {
        System.out.println("Connected to server");
    }
    
    @Override
    public void onLoggedOn(FIXClient client) {
        System.out.println("Logged on - ready to send messages");
    }
    
    @Override
    public void onDisconnected(FIXClient client, String reason) {
        System.out.println("Disconnected: " + reason);
    }
    
    @Override
    public void onError(FIXClient client, Throwable error) {
        System.err.println("Connection error: " + error.getMessage());
    }
    
    @Override
    public void onLoggedOut(FIXClient client) {
        System.out.println("Logged out");
    }
});
```

### Incoming Messages

```java
client.setMessageHandler((message, client) -> {
    String messageType = message.getType();
    
    switch (messageType) {
        case "8": // Execution Report
            handleExecutionReport(message);
            break;
        case "9": // Order Cancel Reject
            handleCancelReject(message);
            break;
        case "j": // Business Message Reject
            handleBusinessReject(message);
            break;
        default:
            System.out.println("Received message: " + messageType);
    }
});
```

## Sending Messages

### Using Order Message Utilities

```java
// Market order
FIXMessage marketOrder = OrderMessage.marketOrder(
    "ORDER_001",                    // Client order ID
    "AAPL",                        // Symbol
    OrderMessage.Side.BUY,         // Side
    new BigDecimal("100")          // Quantity
);

// Limit order
FIXMessage limitOrder = OrderMessage.limitOrder(
    "ORDER_002",                   // Client order ID
    "GOOGL",                       // Symbol
    OrderMessage.Side.SELL,        // Side
    new BigDecimal("50"),          // Quantity
    new BigDecimal("2500.00")      // Price
);

// Send messages
client.sendMessage(marketOrder).get();
client.sendMessage(limitOrder).get();
```

### Custom Messages

```java
FIXMessage customMessage = new FIXMessageImpl();
customMessage.setType("D"); // New Order Single
customMessage.setField("11", "CUSTOM_001");  // ClOrdID
customMessage.setField("55", "MSFT");        // Symbol
customMessage.setField("54", "1");           // Side (Buy)
customMessage.setField("38", "200");         // OrderQty
customMessage.setField("40", "2");           // OrdType (Limit)
customMessage.setField("44", "300.50");      // Price
customMessage.setField("59", "0");           // TimeInForce (Day)

client.sendMessage(customMessage).get();
```

### Request-Response Pattern

```java
// Send message and wait for response
FIXMessage orderStatusRequest = OrderMessage.orderStatusRequest(
    "STATUS_001", "ORDER_001", "AAPL", OrderMessage.Side.BUY);

CompletableFuture<FIXMessage> responseFuture = 
    client.sendAndWaitForResponse(orderStatusRequest);

FIXMessage response = responseFuture.get(30, TimeUnit.SECONDS);
System.out.println("Received response: " + response.getType());
```

## Error Handling

### Connection Errors

```java
try {
    client.connect().get(30, TimeUnit.SECONDS);
} catch (ExecutionException e) {
    if (e.getCause() instanceof FIXClientException) {
        FIXClientException fixError = (FIXClientException) e.getCause();
        System.err.println("FIX connection failed: " + fixError.getMessage());
    }
} catch (TimeoutException e) {
    System.err.println("Connection timeout");
}
```

### Message Sending Errors

```java
client.sendMessage(message)
    .whenComplete((result, throwable) -> {
        if (throwable != null) {
            System.err.println("Failed to send message: " + throwable.getMessage());
        } else {
            System.out.println("Message sent successfully");
        }
    });
```

## Best Practices

### 1. Resource Management

Always shut down the client properly:

```java
try {
    // Use the client
    client.connect().get();
    // ... send messages ...
} finally {
    // Graceful disconnect
    client.disconnect().get(10, TimeUnit.SECONDS);
    
    // Shutdown resources (if using FIXClientImpl)
    if (client instanceof FIXClientImpl) {
        ((FIXClientImpl) client).shutdown();
    }
}
```

### 2. Asynchronous Operations

Use CompletableFuture for non-blocking operations:

```java
client.connect()
    .thenCompose(v -> client.sendMessage(order1))
    .thenCompose(v -> client.sendMessage(order2))
    .thenCompose(v -> client.disconnect())
    .whenComplete((result, throwable) -> {
        if (throwable != null) {
            System.err.println("Operation failed: " + throwable.getMessage());
        } else {
            System.out.println("All operations completed successfully");
        }
    });
```

### 3. Message Validation

Enable message validation in production:

```java
FIXClientConfiguration config = FIXClientConfiguration.builder()
    .validateMessages(true)  // Enable validation
    .build();
```

### 4. Logging

The client uses SLF4J for logging. Configure your logging framework:

```xml
<!-- logback.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.fixserver.client" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

## Testing

### Unit Tests

Run the client unit tests:

```bash
./mvnw test -Dtest=FIXClientImplTest
./mvnw test -Dtest=OrderMessageTest
```

### Integration Testing

For integration testing with a real FIX server:

1. Start your FIX server
2. Update the configuration in the test
3. Run the example client
4. Verify message exchange

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Check if the FIX server is running
   - Verify host and port configuration
   - Check firewall settings

2. **Logon Rejected**
   - Verify SenderCompID and TargetCompID
   - Check username/password if authentication is required
   - Ensure FIX version compatibility

3. **Sequence Number Issues**
   - Use `resetSeqNumFlag(true)` for testing
   - Implement proper sequence number persistence for production

4. **Message Validation Errors**
   - Check required fields for each message type
   - Verify field formats and values
   - Review FIX specification compliance

### Debug Logging

Enable debug logging to see all FIX messages:

```java
// In your logback.xml or log4j configuration
<logger name="com.fixserver.client" level="DEBUG"/>
<logger name="com.fixserver.protocol" level="DEBUG"/>
```

This will show all sent and received FIX messages in human-readable format.

## Examples

See the `FIXClientExample` class for a complete working example that demonstrates:

- Connection management
- Interactive message sending
- Error handling
- Graceful shutdown

The example provides a command-line interface for testing different message types and scenarios.