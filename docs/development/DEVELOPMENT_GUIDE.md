# FIX Server Development Guide

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Development Setup](#development-setup)
5. [Adding New Message Types](#adding-new-message-types)
6. [Extending Protocol Support](#extending-protocol-support)
7. [Session Management](#session-management)
8. [Message Storage](#message-storage)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Troubleshooting](#troubleshooting)

## Overview

This FIX Server is a Java-based implementation of the Financial Information eXchange (FIX) protocol, built using Spring Boot. It provides a robust foundation for handling FIX message processing, session management, and order execution in financial trading systems.

### Key Features
- FIX 4.4 protocol support
- **Dual server implementations:**
  - Traditional socket-based server (blocking I/O)
  - High-performance Netty-based server (non-blocking NIO)
- **Integrated Performance Optimizations:**
  - OptimizedNettyDecoder for 52-99.6% latency reduction
  - HighPerformanceMessageParser with object pooling
  - AsyncMessageStore for non-blocking storage
  - JVM runtime optimizations
- Multi-client session management
- Message persistence and replay
- Comprehensive validation
- Extensible architecture
- Built-in client library
- Production-ready monitoring
- Event-driven message processing (Netty)
- Configurable thread pools and connection settings
- **Automatic performance component selection**
- **Graceful fallback mechanisms**

## Architecture

The FIX Server follows a layered architecture pattern with two connection implementations:

### Traditional Socket-Based Server (Port 9878)
```
┌─────────────────────────────────────────┐
│              Client Layer               │
│        (FIX Client Connections)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Protocol Layer               │
│     (FIX Message Parsing/Formatting)    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Session Layer                │
│      (Session State Management)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Business Layer               │
│        (Order Processing Logic)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Storage Layer                │
│      (Message & Session Persistence)    │
└─────────────────────────────────────────┘
```

### High-Performance Netty-Based Server (Port 9879)
```
┌─────────────────────────────────────────┐
│              Client Layer               │
│         (Netty NIO Channels)            │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Netty Pipeline               │
│    (Decoder → Handler → Encoder)        │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Event Loop Groups               │
│    (Boss Group + Worker Group)          │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Protocol Layer               │
│     (FIX Message Parsing/Formatting)    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Business Layer               │
│        (Order Processing Logic)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Storage Layer                │
│      (Message & Session Persistence)    │
└─────────────────────────────────────────┘
```

## Core Components

### 1. FIX Protocol Handler (`FIXProtocolHandler`)

**Location**: `src/main/java/com/fixserver/protocol/FIXProtocolHandler.java`

Responsible for parsing and formatting FIX messages.

```java
// Parse incoming FIX message
FIXMessage message = protocolHandler.parse(rawMessage);

// Format outgoing FIX message
String fixString = protocolHandler.format(message);
```

**Key Methods**:
- `parse(String rawMessage)`: Converts raw FIX string to FIXMessage object
- `format(FIXMessage message)`: Converts FIXMessage object to FIX string
- `validate(FIXMessage message)`: Validates message structure and content

### 2. FIX Message Implementation (`FIXMessageImpl`)

**Location**: `src/main/java/com/fixserver/core/FIXMessageImpl.java`

Core message representation with field management and validation.

```java
// Create new message
FIXMessage message = new FIXMessageImpl();
message.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
message.setField(FIXMessage.MESSAGE_TYPE, "D"); // New Order Single

// Get field values
String symbol = message.getField(55); // Symbol field
String side = message.getField(54);   // Side field
```

### 3. Protocol Server (`FIXProtocolServer`)

**Location**: `src/main/java/com/fixserver/server/FIXProtocolServer.java`

Main server component that accepts client connections and processes messages.

**Key Features**:
- Multi-threaded client handling
- Complete message detection
- Message routing based on type
- Connection lifecycle management

### 4. Session Management (`FIXSessionImpl`)

**Location**: `src/main/java/com/fixserver/session/FIXSessionImpl.java`

Manages FIX session state, sequence numbers, and heartbeats.

```java
// Session state management
session.processIncomingMessage(message);
session.sendMessage(responseMessage);
session.handleHeartbeat();
```

### 5. Netty-Based Server (`NettyFIXServer`)

**Location**: `src/main/java/com/fixserver/netty/NettyFIXServer.java`

High-performance, non-blocking I/O server implementation using Netty framework.

**Key Features**:
- Event-driven architecture with NIO
- Configurable boss and worker thread pools
- Built-in backpressure handling
- Channel pipeline with custom codecs
- Better scalability for high-throughput scenarios

```java
// Netty server configuration
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("fixDecoder", new FIXMessageDecoder());
                pipeline.addLast("fixEncoder", new FIXMessageEncoder());
                pipeline.addLast("fixHandler", new FIXMessageHandler(protocolHandler));
            }
        });
```

### 6. FIX Message Codec (Netty)

**Decoder**: `src/main/java/com/fixserver/netty/FIXMessageDecoder.java`
- Handles FIX message framing and parsing
- Accumulates bytes until complete message is received
- Validates message structure (BeginString, BodyLength, Checksum)

**Encoder**: `src/main/java/com/fixserver/netty/FIXMessageEncoder.java`
- Converts FIX message strings to bytes for transmission
- Handles proper UTF-8 encoding

## Development Setup

### Prerequisites
- Java 8 or higher
- Maven 3.6+
- PostgreSQL (optional, for persistence)
- Git

### Environment Setup

1. **Clone and Setup**:
```bash
git clone <repository-url>
cd fix-server
source setup-env.sh
```

2. **Build Project**:
```bash
./mvnw clean compile
```

3. **Run Tests**:
```bash
./mvnw test
```

4. **Start Server**:
```bash
./mvnw spring-boot:run
```

### Configuration

**Application Configuration** (`src/main/resources/application.yml`):
```yaml
fix:
  server:
    port: 9878                    # Traditional socket server port
    max-sessions: 100
    
    # Netty-based server configuration
    netty:
      enabled: true               # Enable/disable Netty server
      port: 9879                  # Netty server port
      boss-threads: 1             # Acceptor threads
      worker-threads: 0           # I/O threads (0 = auto)
      backlog: 128                # Connection backlog
      receive-buffer-size: 32768  # SO_RCVBUF
      send-buffer-size: 32768     # SO_SNDBUF
      keep-alive: true            # SO_KEEPALIVE
      tcp-no-delay: true          # TCP_NODELAY
      max-sessions: 1000          # Max concurrent sessions
    
    tls:
      enabled: false

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fixserver
    username: fixuser
    password: fixpass
```

## Adding New Message Types

### Step 1: Define Message Type

Add new message type to `MessageType` enum:

```java
// src/main/java/com/fixserver/protocol/MessageType.java
public enum MessageType {
    // Existing types...
    QUOTE_REQUEST("R", "Quote Request"),
    QUOTE("S", "Quote");
    
    // Constructor and methods...
}
```

### Step 2: Add Field Definitions

Define new fields in `FieldDefinition`:

```java
// src/main/java/com/fixserver/protocol/FieldDefinition.java
public class FieldDefinition {
    // Existing fields...
    public static final int QUOTE_REQ_ID = 131;
    public static final int BID_PX = 132;
    public static final int OFFER_PX = 133;
}
```

### Step 3: Implement Message Handler

Add handler in `FIXProtocolServer`:

```java
// src/main/java/com/fixserver/server/FIXProtocolServer.java
private void handleQuoteRequest(FIXMessage quoteRequest, BufferedWriter writer) throws Exception {
    String quoteReqId = quoteRequest.getField(131); // QuoteReqID
    String symbol = quoteRequest.getField(55);      // Symbol
    
    // Create quote response
    FIXMessage quote = new FIXMessageImpl();
    quote.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
    quote.setField(FIXMessage.MESSAGE_TYPE, "S"); // Quote
    quote.setField(FIXMessage.SENDER_COMP_ID, targetCompId);
    quote.setField(FIXMessage.TARGET_COMP_ID, senderCompId);
    quote.setField(131, quoteReqId); // QuoteReqID
    quote.setField(55, symbol);      // Symbol
    quote.setField(132, "100.50");   // BidPx
    quote.setField(133, "100.55");   // OfferPx
    
    String fixString = quote.toFixString();
    writer.write(fixString);
    writer.flush();
    
    log.info("Sent quote for symbol {}: {}", symbol, fixString.replace('\u0001', '|'));
}
```

### Step 4: Add Message Routing

Update message routing logic:

```java
// In handleClientConnection method
else if ("R".equals(messageType)) {
    // Quote Request
    handleQuoteRequest(message, writer);
    log.info("Handled quote request from {}", clientAddress);
}
```

### Step 5: Add Validation Rules

Extend `FIXValidator` for new message validation:

```java
// src/main/java/com/fixserver/protocol/FIXValidator.java
private void validateQuoteRequest(FIXMessage message) {
    // Required fields validation
    validateRequiredField(message, 131, "QuoteReqID");
    validateRequiredField(message, 55, "Symbol");
    
    // Business logic validation
    String symbol = message.getField(55);
    if (symbol == null || symbol.trim().isEmpty()) {
        addError("Symbol cannot be empty");
    }
}
```

## Extending Protocol Support

### Adding New FIX Versions

1. **Update Protocol Handler**:
```java
// Support multiple FIX versions
public class FIXProtocolHandler {
    private static final Map<String, ProtocolVersion> SUPPORTED_VERSIONS = Map.of(
        "FIX.4.2", new FIX42Handler(),
        "FIX.4.4", new FIX44Handler(),
        "FIX.5.0", new FIX50Handler()
    );
}
```

2. **Version-Specific Parsing**:
```java
public FIXMessage parse(String rawMessage) throws FIXParseException {
    String beginString = extractBeginString(rawMessage);
    ProtocolVersion handler = SUPPORTED_VERSIONS.get(beginString);
    
    if (handler == null) {
        throw new FIXParseException("Unsupported FIX version: " + beginString);
    }
    
    return handler.parse(rawMessage);
}
```

### Custom Field Types

1. **Define Custom Fields**:
```java
public class CustomFieldDefinition extends FieldDefinition {
    public static final int CUSTOM_FIELD_1 = 9001;
    public static final int CUSTOM_FIELD_2 = 9002;
}
```

2. **Add Custom Validation**:
```java
public class CustomValidator extends FIXValidator {
    @Override
    protected void validateCustomFields(FIXMessage message) {
        // Custom validation logic
        String customValue = message.getField(9001);
        if (customValue != null && !isValidCustomValue(customValue)) {
            addError("Invalid custom field value: " + customValue);
        }
    }
}
```

## Session Management

### Custom Session Behavior

Extend `FIXSessionImpl` for custom session handling:

```java
public class CustomFIXSession extends FIXSessionImpl {
    
    @Override
    public void processIncomingMessage(FIXMessage message) throws Exception {
        // Custom pre-processing
        logCustomMetrics(message);
        
        // Call parent implementation
        super.processIncomingMessage(message);
        
        // Custom post-processing
        updateCustomState(message);
    }
    
    private void logCustomMetrics(FIXMessage message) {
        // Custom metrics collection
        String messageType = message.getMessageType();
        metricsCollector.incrementCounter("messages.received." + messageType);
    }
}
```

### Session State Persistence

Implement custom session state storage:

```java
@Component
public class DatabaseSessionManager implements SessionManager {
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Override
    public void saveSessionState(String sessionId, SessionState state) {
        SessionEntity entity = new SessionEntity();
        entity.setSessionId(sessionId);
        entity.setState(state.toString());
        entity.setLastActivity(LocalDateTime.now());
        
        sessionRepository.save(entity);
    }
    
    @Override
    public SessionState loadSessionState(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
            .map(entity -> SessionState.fromString(entity.getState()))
            .orElse(new SessionState());
    }
}
```

## Message Storage

### Custom Message Store Implementation

```java
@Component
public class CustomMessageStore implements MessageStore {
    
    @Override
    public void storeMessage(String sessionId, FIXMessage message, MessageDirection direction) {
        MessageEntity entity = new MessageEntity();
        entity.setSessionId(sessionId);
        entity.setMessageType(message.getMessageType());
        entity.setDirection(direction);
        entity.setRawMessage(message.toFixString());
        entity.setSequenceNumber(message.getMessageSequenceNumber());
        entity.setTimestamp(LocalDateTime.now());
        
        // Add custom fields
        entity.setSymbol(message.getField(55)); // Symbol
        entity.setOrderId(message.getField(37)); // OrderID
        
        messageRepository.save(entity);
    }
    
    @Override
    public List<FIXMessage> getMessages(String sessionId, int fromSeqNum, int toSeqNum) {
        return messageRepository.findBySessionIdAndSequenceNumberBetween(
            sessionId, fromSeqNum, toSeqNum)
            .stream()
            .map(this::entityToMessage)
            .collect(Collectors.toList());
    }
}
```

### Message Replay Service

```java
@Service
public class MessageReplayService {
    
    public void replayMessages(String sessionId, int fromSeqNum, int toSeqNum) {
        List<FIXMessage> messages = messageStore.getMessages(sessionId, fromSeqNum, toSeqNum);
        
        FIXSession session = sessionManager.getSession(sessionId);
        for (FIXMessage message : messages) {
            // Resend message with PossDupFlag set
            message.setField(43, "Y"); // PossDupFlag
            session.sendMessage(message);
        }
    }
}
```

## Testing

### Unit Testing

**Test FIX Message Creation**:
```java
@Test
public void testMessageCreation() {
    FIXMessage message = new FIXMessageImpl();
    message.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
    message.setField(FIXMessage.MESSAGE_TYPE, "D");
    message.setField(55, "AAPL"); // Symbol
    
    assertEquals("FIX.4.4", message.getBeginString());
    assertEquals("D", message.getMessageType());
    assertEquals("AAPL", message.getField(55));
}
```

**Test Protocol Handler**:
```java
@Test
public void testMessageParsing() throws FIXParseException {
    String rawMessage = "8=FIX.4.4\u00019=49\u000135=D\u000149=CLIENT\u000156=SERVER\u000155=AAPL\u000110=123\u0001";
    
    FIXMessage message = protocolHandler.parse(rawMessage);
    
    assertEquals("FIX.4.4", message.getBeginString());
    assertEquals("D", message.getMessageType());
    assertEquals("AAPL", message.getField(55));
}
```

### Integration Testing

**Test Client-Server Communication**:
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class FIXServerIntegrationTest {
    
    @Test
    @Order(1)
    public void testLogonSequence() throws Exception {
        FIXClient client = FIXClientFactory.create(config);
        client.connect().get(5, TimeUnit.SECONDS);
        
        assertTrue(client.isConnected());
        assertEquals("CLIENT1-SERVER1", client.getSessionId());
    }
    
    @Test
    @Order(2)
    public void testOrderSubmission() throws Exception {
        FIXMessage order = OrderMessage.newOrderSingle(
            "ORDER1", "AAPL", Side.BUY, 
            BigDecimal.valueOf(100), OrderType.MARKET, 
            null, TimeInForce.DAY
        );
        
        CompletableFuture<FIXMessage> response = client.sendMessage(order);
        FIXMessage executionReport = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("8", executionReport.getMessageType()); // Execution Report
        assertEquals("ORDER1", executionReport.getField(11)); // ClOrdID
    }
}
```

### Load Testing

```java
@Test
public void testConcurrentClients() throws Exception {
    int clientCount = 50;
    ExecutorService executor = Executors.newFixedThreadPool(clientCount);
    CountDownLatch latch = new CountDownLatch(clientCount);
    
    for (int i = 0; i < clientCount; i++) {
        final int clientId = i;
        executor.submit(() -> {
            try {
                FIXClient client = createClient("CLIENT" + clientId);
                client.connect().get(10, TimeUnit.SECONDS);
                
                // Send multiple orders
                for (int j = 0; j < 10; j++) {
                    sendTestOrder(client, "ORDER" + clientId + "_" + j);
                }
                
                client.disconnect().get(5, TimeUnit.SECONDS);
            } finally {
                latch.countDown();
            }
        });
    }
    
    assertTrue(latch.await(60, TimeUnit.SECONDS));
}
```

### Testing Netty Implementation

**Run Netty Client Example**:
```bash
# Using shell script
./run-netty-client.sh localhost 9879 CLIENT1 SERVER1

# Using Java directly
java -cp "target/classes:$(./mvnw dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.fixserver.netty.NettyFIXClientExample localhost 9879 CLIENT1 SERVER1
```

**Netty Performance Testing**:
```java
@Test
public void testNettyServerPerformance() throws Exception {
    // Connect multiple clients to Netty server (port 9879)
    int clientCount = 100;
    CountDownLatch connectLatch = new CountDownLatch(clientCount);
    
    for (int i = 0; i < clientCount; i++) {
        new Thread(() -> {
            try {
                NettyFIXClientExample client = new NettyFIXClientExample(
                    "localhost", 9879, "CLIENT" + Thread.currentThread().getId(), "SERVER1");
                client.connect();
            } finally {
                connectLatch.countDown();
            }
        }).start();
    }
    
    assertTrue(connectLatch.await(30, TimeUnit.SECONDS));
}
```

## Performance Integration

### Overview

The FIX Server v3.0 integrates high-performance components directly into the message processing workflow, providing automatic optimization without configuration complexity.

### Performance Components

#### **1. OptimizedNettyDecoder**
Replaces the standard FIXMessageDecoder in the Netty pipeline:

```java
// Automatic selection in NettyFIXServer
if (performanceOptimizationsEnabled) {
    pipeline.addLast("fixDecoder", new OptimizedNettyDecoder(highPerformanceParser));
} else {
    pipeline.addLast("fixDecoder", new FIXMessageDecoder());
}
```

**Benefits:**
- Zero-copy ByteBuf operations
- Direct memory access for parsing
- Reduced object allocation
- 52% latency improvement

#### **2. HighPerformanceMessageParser**
Integrated into message processing workflow:

```java
// Automatic parser selection in FIXMessageHandler
if (performanceOptimizationsEnabled && highPerformanceParser != null) {
    message = highPerformanceParser.parse(rawMessage);
} else {
    message = protocolHandler.parse(rawMessage);
}
```

**Features:**
- Object pooling for reduced GC pressure
- Pre-allocated field arrays
- Cached field lookups
- Thread-local storage optimization

#### **3. AsyncMessageStore**
Non-blocking message storage:

```java
// Automatic async storage in message handler
if (performanceOptimizationsEnabled && asyncMessageStore != null) {
    asyncMessageStore.storeMessage(message);
}
```

**Capabilities:**
- Ring buffer-based storage (65,536 entries default)
- Batch processing (100 messages default)
- Configurable flush intervals (50ms default)
- Non-blocking message persistence

#### **4. JVMOptimizationConfig**
Runtime JVM optimizations:

```java
// Applied automatically on server startup
if (performanceOptimizationsEnabled && jvmOptimizationConfig != null) {
    jvmOptimizationConfig.applyOptimizations();
}
```

**Optimizations:**
- G1GC configuration
- Memory pool tuning
- Thread optimization
- GC monitoring setup

### Configuration

#### **Enable Performance Optimizations**
```yaml
fix:
  server:
    performance:
      enabled: true                    # Master switch
      use-optimized-parser: true       # High-performance parser
      use-async-storage: true          # Async message storage
      object-pooling-enabled: true     # Object pooling
      ring-buffer-size: 65536          # Ring buffer size
      batch-size: 100                  # Batch processing size
      flush-interval-ms: 50            # Low-latency flush
```

#### **Netty Performance Settings**
```yaml
fix:
  server:
    netty:
      pooled-allocator: true           # Pooled ByteBuf allocator
      direct-memory: true              # Direct memory usage
      write-spin-count: 16             # Write optimization
      receive-buffer-size: 65536       # 64KB receive buffer
      send-buffer-size: 65536          # 64KB send buffer
```

### Development Integration

#### **Adding Performance-Aware Components**

When developing new components, follow the performance integration pattern:

```java
@Component
public class MyMessageProcessor {
    
    @Autowired(required = false)
    private HighPerformanceMessageParser highPerformanceParser;
    
    @Value("${fix.server.performance.enabled:true}")
    private boolean performanceOptimizationsEnabled;
    
    public void processMessage(String rawMessage) {
        // Use optimized components when available
        if (performanceOptimizationsEnabled && highPerformanceParser != null) {
            // Use high-performance path
            FIXMessage message = highPerformanceParser.parse(rawMessage);
            processOptimized(message);
        } else {
            // Use standard path
            FIXMessage message = standardParser.parse(rawMessage);
            processStandard(message);
        }
    }
}
```

#### **Performance Testing Integration**

```java
@Test
public void testPerformanceIntegration() {
    // Verify optimized components are loaded
    assertNotNull(highPerformanceParser);
    assertNotNull(asyncMessageStore);
    
    // Test performance characteristics
    long startTime = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        processMessage(testMessage);
    }
    long duration = System.nanoTime() - startTime;
    
    // Verify performance targets
    assertTrue(duration < PERFORMANCE_THRESHOLD);
}
```

#### **Monitoring Integration**

```java
@RestController
@RequestMapping("/admin/performance")
public class PerformanceController {
    
    @GetMapping("/status")
    public PerformanceStatus getStatus() {
        return PerformanceStatus.builder()
            .optimizationsEnabled(performanceOptimizationsEnabled)
            .highPerformanceParserActive(highPerformanceParser != null)
            .asyncStorageActive(asyncMessageStore != null)
            .currentThroughput(performanceOptimizer.getCurrentThroughput())
            .averageLatency(performanceOptimizer.getAverageLatency())
            .build();
    }
}
```

### Performance Metrics

The integrated performance system provides comprehensive metrics:

#### **Message Processing Metrics**
- Total messages processed
- Average/min/max processing time
- Message throughput (messages/second)
- Data throughput (MB/second)

#### **Component Performance**
- Parser selection statistics
- Async storage queue depth
- Object pool utilization
- Memory allocation rates

#### **System Performance**
- JVM optimization status
- GC performance metrics
- Memory usage patterns
- Thread pool utilization

### Troubleshooting Performance Issues

#### **Common Issues and Solutions**

1. **High Latency Despite Optimizations**
   ```bash
   # Check if optimizations are enabled
   curl http://localhost:8080/admin/performance/status
   
   # Verify component loading
   grep "HighPerformanceMessageParser" logs/fix-server.log
   ```

2. **Memory Issues with Async Storage**
   ```yaml
   # Reduce ring buffer size
   fix.server.performance.ring-buffer-size: 32768
   
   # Increase flush frequency
   fix.server.performance.flush-interval-ms: 25
   ```

3. **Parser Fallback Issues**
   ```bash
   # Check for parser errors
   grep "falling back to standard parser" logs/fix-server.log
   
   # Verify message format compatibility
   ```

## Deployment

### Production Configuration

**application-prod.yml**:
```yaml
fix:
  server:
    port: 9878
    max-sessions: 1000
    tls:
      enabled: true
      keystore: /etc/ssl/fixserver.jks
      keystore-password: ${KEYSTORE_PASSWORD}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/fixserver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

logging:
  level:
    com.fixserver: INFO
    org.springframework: WARN
  file:
    name: /var/log/fixserver/application.log
```

### Docker Deployment

**Dockerfile**:
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app
COPY target/fix-server-*.jar app.jar
COPY docker-entrypoint.sh .

RUN chmod +x docker-entrypoint.sh

EXPOSE 9878 8080

ENTRYPOINT ["./docker-entrypoint.sh"]
```

**docker-compose.yml**:
```yaml
version: '3.8'
services:
  fix-server:
    build: .
    ports:
      - "9878:9878"
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - DB_USERNAME=fixuser
      - DB_PASSWORD=fixpass
    depends_on:
      - postgres
    
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=fixserver
      - POSTGRES_USER=fixuser
      - POSTGRES_PASSWORD=fixpass
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### Monitoring

**Health Checks**:
```java
@Component
public class FIXServerHealthIndicator implements HealthIndicator {
    
    @Autowired
    private FIXProtocolServer server;
    
    @Override
    public Health health() {
        if (server.isRunning()) {
            return Health.up()
                .withDetail("port", server.getPort())
                .withDetail("active-sessions", server.getActiveSessionCount())
                .build();
        } else {
            return Health.down()
                .withDetail("reason", "FIX server not running")
                .build();
        }
    }
}
```

**Custom Metrics**:
```java
@Component
public class FIXMetrics {
    
    private final Counter messagesReceived;
    private final Counter messagesSent;
    private final Timer messageProcessingTime;
    
    public FIXMetrics(MeterRegistry meterRegistry) {
        this.messagesReceived = Counter.builder("fix.messages.received")
            .tag("type", "all")
            .register(meterRegistry);
            
        this.messagesSent = Counter.builder("fix.messages.sent")
            .tag("type", "all")
            .register(meterRegistry);
            
        this.messageProcessingTime = Timer.builder("fix.message.processing.time")
            .register(meterRegistry);
    }
}
```

## Troubleshooting

### Common Issues

**1. Message Parsing Errors**
```
Error: Message missing required header fields
```
**Solution**: Ensure messages include BeginString (8), BodyLength (9), and Checksum (10) fields.

**2. Sequence Number Mismatches**
```
Warning: Sequence number mismatch: expected 5, got 7
```
**Solution**: Implement proper sequence number management and gap fill handling.

**3. Connection Timeouts**
```
Error: Connection timeout during logon
```
**Solution**: Check network connectivity and increase timeout values in configuration.

### Debug Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.fixserver.protocol: DEBUG
    com.fixserver.session: DEBUG
    com.fixserver.server: DEBUG
```

### Message Tracing

Add message tracing for debugging:

```java
@Component
public class MessageTracer {
    
    public void traceIncoming(String sessionId, String rawMessage) {
        log.debug("RECV [{}]: {}", sessionId, rawMessage.replace('\u0001', '|'));
    }
    
    public void traceOutgoing(String sessionId, String rawMessage) {
        log.debug("SEND [{}]: {}", sessionId, rawMessage.replace('\u0001', '|'));
    }
}
```

### Performance Tuning

**JVM Options**:
```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
     -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
     -jar fix-server.jar
```

**Connection Pool Tuning**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Choosing Between Socket and Netty Implementations

### Traditional Socket Server (Port 9878)
**Use When:**
- Simple deployment requirements
- Lower concurrent connection count (< 100 clients)
- Blocking I/O is acceptable
- Easier debugging and troubleshooting needed

**Characteristics:**
- One thread per connection
- Blocking I/O operations
- Simpler code structure
- Lower memory overhead per connection

### Netty Server (Port 9879)
**Use When:**
- High concurrent connection count (> 100 clients)
- Low latency requirements
- High throughput scenarios
- Non-blocking I/O benefits needed

**Characteristics:**
- Event-driven, non-blocking I/O
- Configurable thread pools
- Better resource utilization
- Higher throughput and scalability

### Performance Comparison
```
Concurrent Connections | Socket Server | Netty Server
----------------------|---------------|-------------
1-50 clients          | Good          | Excellent
51-200 clients        | Fair          | Excellent  
201-1000 clients      | Poor          | Excellent
1000+ clients         | Not Recommended | Good
```

## Best Practices

1. **Always validate messages** before processing
2. **Use proper sequence number management** for session integrity
3. **Implement comprehensive logging** for audit trails
4. **Handle network failures gracefully** with reconnection logic
5. **Use connection pooling** for database operations
6. **Monitor performance metrics** in production
7. **Implement proper security** for production deployments
8. **Test thoroughly** with various message scenarios
9. **Follow FIX protocol specifications** strictly
10. **Document custom extensions** clearly
11. **Choose appropriate server implementation** based on requirements
12. **Configure Netty thread pools** based on expected load
13. **Monitor both servers** if running simultaneously

## Resources

- [FIX Protocol Specification](https://www.fixtrading.org/standards/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Project Repository](https://github.com/your-org/fix-server)
- [Issue Tracker](https://github.com/your-org/fix-server/issues)

---

For additional support or questions, please refer to the project documentation or create an issue in the repository.