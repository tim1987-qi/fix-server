# FIX Server Architecture and Message Flow

This document provides a comprehensive flowchart of the FIX server architecture, showing server startup, client connection, and message processing flows at the class level.

## Overall Architecture Flowchart

```mermaid
graph TB
    %% Server Startup Flow
    subgraph "Server Startup"
        A[FIXServerApplication.main] --> B[Spring Boot Context]
        B --> C[MessageStoreConfiguration]
        C --> D[InMemoryMessageStore / MessageStoreImpl]
        B --> E[NettyConfiguration]
        E --> F[NettyFIXServer]
        B --> G[FIXProtocolServer]
        F --> H[ServerBootstrap.bind:9879]
        G --> I[ServerSocket.bind:9878]
    end

    %% Client Connection Flow
    subgraph "Client Connection"
        J[Client Socket] --> K{Server Type?}
        K -->|Netty| L[NettyFIXServer.channelActive]
        K -->|Traditional| M[FIXProtocolServer.accept]
        L --> N[FIXMessageHandler.channelActive]
        M --> O[ClientHandler Thread]
        N --> P[NettyFIXSession.create]
        O --> Q[FIXSessionImpl.create]
    end

    %% Message Processing Flow
    subgraph "Message Processing"
        R[Raw FIX Message] --> S{Server Type?}
        S -->|Netty| T[FIXMessageDecoder.decode]
        S -->|Traditional| U[FIXProtocolHandler.parseMessage]
        T --> V[FIXMessageHandler.channelRead]
        U --> W[FIXSessionImpl.processMessage]
        V --> X[NettyFIXSession.processMessage]
        W --> Y[FIXProtocolHandler.handleMessage]
        X --> Y
        Y --> Z[MessageStore.storeMessage]
        Y --> AA[Response Generation]
        AA --> BB[FIXMessageEncoder.encode]
        BB --> CC[Client Response]
    end

    %% Connections
    H --> L
    I --> M
    P --> X
    Q --> W
```

## Detailed Server Startup Flow

```mermaid
sequenceDiagram
    participant Main as FIXServerApplication
    participant Spring as Spring Context
    participant Config as Configuration Classes
    participant NettyServer as NettyFIXServer
    participant TraditionalServer as FIXProtocolServer
    participant Store as MessageStore

    Main->>Spring: @SpringBootApplication.run()
    Spring->>Config: Load @Configuration classes
    Config->>Store: Create MessageStore instance
    Note over Store: InMemoryMessageStore or<br/>MessageStoreImpl (with DB)
    
    Spring->>NettyServer: @PostConstruct start()
    NettyServer->>NettyServer: Configure EventLoopGroup
    NettyServer->>NettyServer: Setup ChannelPipeline
    Note over NettyServer: Add FIXMessageDecoder<br/>Add FIXMessageEncoder<br/>Add FIXMessageHandler
    NettyServer->>NettyServer: Bind to port 9879
    
    Spring->>TraditionalServer: @PostConstruct start()
    TraditionalServer->>TraditionalServer: Create ServerSocket
    TraditionalServer->>TraditionalServer: Bind to port 9878
    TraditionalServer->>TraditionalServer: Start acceptor thread
    
    Note over Main: Both servers now listening<br/>Ready for client connections
```

## Client Connection and Session Establishment

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant NettyServer as NettyFIXServer
    participant Handler as FIXMessageHandler
    participant Session as NettyFIXSession
    participant SessionMgr as SessionManager
    participant Store as MessageStore

    Client->>NettyServer: TCP Connection (port 9879)
    NettyServer->>Handler: channelActive(ctx)
    Handler->>Handler: Log connection from client
    
    Client->>Handler: FIX Logon Message (35=A)
    Handler->>Handler: channelRead(ctx, msg)
    Handler->>Session: Create new NettyFIXSession
    Session->>SessionMgr: Register session
    Session->>Store: Initialize session storage
    
    Session->>Session: Validate logon message
    Session->>Session: Generate logon response
    Session->>Handler: Send logon response (35=A)
    Handler->>Client: FIX Logon Response
    
    Note over Session: Session established<br/>Ready for business messages
```

## Traditional Server Connection Flow

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant Server as FIXProtocolServer
    participant Handler as ClientHandler Thread
    participant Session as FIXSessionImpl
    participant Protocol as FIXProtocolHandler
    participant Store as MessageStore

    Client->>Server: TCP Connection (port 9878)
    Server->>Handler: Accept connection
    Handler->>Handler: Create new thread for client
    
    Client->>Handler: FIX Logon Message (35=A)
    Handler->>Protocol: parseMessage(rawMessage)
    Protocol->>Protocol: Validate FIX format
    Protocol->>Session: Create FIXSessionImpl
    Session->>Store: Initialize session
    
    Session->>Protocol: Process logon message
    Protocol->>Session: Generate logon response
    Session->>Handler: Send response
    Handler->>Client: FIX Logon Response
    
    Note over Session: Session established<br/>Ready for business messages
```

## Message Processing Flow (Netty)

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant Decoder as FIXMessageDecoder
    participant Handler as FIXMessageHandler
    participant Session as NettyFIXSession
    participant Protocol as FIXProtocolHandler
    participant Validator as FIXValidator
    participant Store as MessageStore
    participant Encoder as FIXMessageEncoder

    Client->>Decoder: Raw FIX Message bytes
    Decoder->>Decoder: Parse FIX message structure
    Decoder->>Decoder: Validate message format
    Decoder->>Handler: Decoded FIX message
    
    Handler->>Session: processMessage(fixMessage)
    Session->>Protocol: handleMessage(message)
    Protocol->>Validator: validateMessage(message)
    Validator->>Validator: Check required fields
    Validator->>Validator: Validate field formats
    
    alt Valid Message
        Protocol->>Store: storeMessage(message)
        Protocol->>Protocol: Generate business response
        Protocol->>Session: sendResponse(response)
        Session->>Encoder: encode(response)
        Encoder->>Client: Encoded FIX response
    else Invalid Message
        Protocol->>Protocol: Generate reject message
        Protocol->>Session: sendReject(reject)
        Session->>Encoder: encode(reject)
        Encoder->>Client: FIX Reject message
    end
```

## Message Processing Flow (Traditional)

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant Handler as ClientHandler Thread
    participant Protocol as FIXProtocolHandler
    participant Session as FIXSessionImpl
    participant Validator as FIXValidator
    participant Store as MessageStore

    Client->>Handler: Raw FIX Message
    Handler->>Protocol: parseMessage(rawMessage)
    Protocol->>Protocol: Split fields by SOH
    Protocol->>Protocol: Create FIXMessage object
    
    Protocol->>Session: processMessage(fixMessage)
    Session->>Validator: validateMessage(message)
    Validator->>Validator: Validate message structure
    
    alt Valid Message
        Session->>Store: storeMessage(message)
        Session->>Session: processBusinessLogic(message)
        Session->>Protocol: generateResponse(message)
        Protocol->>Handler: formatResponse(response)
        Handler->>Client: FIX Response
    else Invalid Message
        Session->>Protocol: generateReject(message)
        Protocol->>Handler: formatReject(reject)
        Handler->>Client: FIX Reject
    end
```

## Session Management Flow

```mermaid
graph TB
    subgraph "Session Lifecycle"
        A[Client Connection] --> B[Session Creation]
        B --> C[Logon Processing]
        C --> D{Logon Valid?}
        D -->|Yes| E[Session Active]
        D -->|No| F[Reject & Disconnect]
        E --> G[Message Processing]
        G --> H[Heartbeat Management]
        H --> I{Session Timeout?}
        I -->|No| G
        I -->|Yes| J[Session Cleanup]
        F --> J
        J --> K[Connection Closed]
    end

    subgraph "Session Components"
        L[SessionManager] --> M[FIXSessionImpl / NettyFIXSession]
        M --> N[HeartbeatManager]
        M --> O[SessionTimeoutHandler]
        M --> P[SessionState]
        N --> Q[Periodic Heartbeat Check]
        O --> R[Timeout Detection]
        P --> S[CONNECTING/ACTIVE/DISCONNECTING]
    end
```

## Detailed Heartbeat Management Flow

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant Session as FIX Session
    participant HBManager as HeartbeatManager
    participant TimeoutHandler as SessionTimeoutHandler
    participant Scheduler as ScheduledExecutor

    Note over HBManager: HeartbeatManager starts with application
    HBManager->>Scheduler: Schedule heartbeat checks (every 10s)
    HBManager->>Scheduler: Schedule timeout checks (every 10s)
    
    Note over Session: Session established with heartbeat interval (e.g., 30s)
    Session->>HBManager: registerSession(session, callback)
    Session->>TimeoutHandler: registerSession(sessionId, callback)
    
    loop Every 10 seconds
        Scheduler->>HBManager: checkHeartbeats()
        HBManager->>HBManager: Check if heartbeat needed
        
        alt Heartbeat interval exceeded
            HBManager->>Session: Send Heartbeat (35=0)
            Session->>Client: Heartbeat message
            HBManager->>HBManager: Update lastHeartbeatSent
        end
        
        alt No heartbeat received for 1.5x interval
            HBManager->>Session: Send Test Request (35=1)
            Session->>Client: Test Request with TestReqID
            HBManager->>HBManager: Set pendingTestReqId
        end
        
        alt No response to Test Request for 2x interval
            HBManager->>Session: Trigger timeout
            Session->>TimeoutHandler: onSessionTimeout()
            TimeoutHandler->>Session: Cleanup and disconnect
        end
    end
    
    Note over Client: Client sends any message
    Client->>Session: Any FIX message
    Session->>HBManager: updateHeartbeat(sessionId)
    Session->>TimeoutHandler: updateActivity(sessionId)
    HBManager->>HBManager: Clear pendingTestReqId
```

## Connection Loss and Recovery Flow

```mermaid
graph TB
    subgraph "Connection Monitoring"
        A[Active Session] --> B[Network Monitoring]
        B --> C{Connection Status}
        C -->|Healthy| D[Continue Processing]
        C -->|Lost| E[Connection Lost Event]
        D --> B
    end

    subgraph "Server-Side Connection Loss"
        E --> F[FIXMessageHandler.channelInactive]
        F --> G[NettyFIXSession.onDisconnect]
        G --> H[SessionManager.removeSession]
        H --> I[HeartbeatManager.unregisterSession]
        I --> J[SessionTimeoutHandler.unregisterSession]
        J --> K[MessageStore.updateSessionStatus]
        K --> L[Log Disconnection Event]
    end

    subgraph "Client-Side Reconnection"
        M[Client Detects Disconnection] --> N[FIXClientConnectionHandler.onDisconnected]
        N --> O{Auto Reconnect?}
        O -->|Yes| P[Wait Reconnect Delay]
        O -->|No| Q[Manual Reconnection Required]
        P --> R[Attempt Reconnection]
        R --> S{Connection Success?}
        S -->|Yes| T[Re-establish Session]
        S -->|No| U{Max Retries?}
        U -->|No| V[Exponential Backoff]
        U -->|Yes| W[Give Up - Manual Intervention]
        V --> R
    end

    subgraph "Session Recovery"
        T --> X[Send Logon Message]
        X --> Y[Check Sequence Numbers]
        Y --> Z{Gap Detected?}
        Z -->|Yes| AA[Request Message Replay]
        Z -->|No| BB[Resume Normal Processing]
        AA --> CC[GapFillManager.processGap]
        CC --> DD[MessageReplayService.replayMessages]
        DD --> BB
    end
```

## Heartbeat State Machine

```mermaid
stateDiagram-v2
    [*] --> Registered : registerSession()
    
    Registered --> Normal : Session Active
    Normal --> HeartbeatDue : Heartbeat interval exceeded
    HeartbeatDue --> Normal : Send Heartbeat (35=0)
    
    Normal --> TestRequestSent : No heartbeat for 1.5x interval
    TestRequestSent --> Normal : Receive any message
    TestRequestSent --> TimedOut : No response for 2x interval
    
    TimedOut --> Cleanup : Trigger session timeout
    Cleanup --> [*] : unregisterSession()
    
    Normal --> Cleanup : Manual disconnect
    HeartbeatDue --> Cleanup : Session error
    TestRequestSent --> Cleanup : Connection lost
```

## Connection Recovery Strategies

```mermaid
graph TB
    subgraph "Client Reconnection Logic"
        A[Connection Lost] --> B[FIXClientConnectionHandler.onDisconnected]
        B --> C{Reconnect Policy}
        C -->|Immediate| D[Immediate Retry]
        C -->|Delayed| E[Fixed Delay]
        C -->|Exponential| F[Exponential Backoff]
        C -->|Manual| G[Wait for Manual Trigger]
        
        D --> H[Attempt Connection]
        E --> I[Wait Delay Period]
        F --> J[Calculate Backoff Delay]
        I --> H
        J --> K[Wait Backoff Period]
        K --> H
        
        H --> L{Connection Success?}
        L -->|Yes| M[Connection Established]
        L -->|No| N{Retry Count < Max?}
        N -->|Yes| O[Increment Retry Count]
        N -->|No| P[Max Retries Exceeded]
        O --> C
        P --> Q[Connection Failed - Manual Intervention]
        
        M --> R[Send Logon Message]
        R --> S[Session Recovery Process]
    end

    subgraph "Server Session Recovery"
        T[New Connection from Known Client] --> U[Check Session History]
        U --> V{Previous Session Exists?}
        V -->|Yes| W[Load Session State]
        V -->|No| X[Create New Session]
        W --> Y[Compare Sequence Numbers]
        Y --> Z{Sequence Gap?}
        Z -->|Yes| AA[Initiate Gap Fill]
        Z -->|No| BB[Resume Session]
        AA --> CC[Send Gap Fill Messages]
        CC --> BB
        X --> BB
    end
```

## Message Store Architecture

```mermaid
graph TB
    subgraph "Storage Layer"
        A[MessageStore Interface] --> B{Implementation}
        B -->|Development| C[InMemoryMessageStore]
        B -->|Production| D[MessageStoreImpl]
        
        D --> E[MessageRepository]
        D --> F[SessionRepository]
        D --> G[AuditRepository]
        
        E --> H[MessageEntity]
        F --> I[SessionEntity]
        G --> J[AuditRecordEntity]
        
        H --> K[Database Tables]
        I --> K
        J --> K
    end

    subgraph "Message Replay"
        L[MessageReplayService] --> M[GapFillManager]
        L --> N[MessageStore]
        M --> O[Gap Detection]
        M --> P[Resend Requests]
    end
```

## Error Handling Flow

```mermaid
graph TB
    subgraph "Error Handling"
        A[Message Received] --> B[FIXValidator.validate]
        B --> C{Valid?}
        C -->|Yes| D[Process Message]
        C -->|No| E[Generate Reject]
        
        E --> F{Error Type}
        F -->|Format Error| G[BusinessMessageReject]
        F -->|Session Error| H[SessionReject]
        F -->|Protocol Error| I[Logout]
        
        G --> J[Send Reject Message]
        H --> J
        I --> K[Terminate Session]
        
        D --> L{Processing Error?}
        L -->|Yes| M[BusinessReject]
        L -->|No| N[Send Response]
        M --> J
    end
```

## Key Classes and Their Responsibilities

### Server Components
- **FIXServerApplication**: Main Spring Boot application entry point
- **NettyFIXServer**: High-performance Netty-based FIX server (port 9879)
- **FIXProtocolServer**: Traditional socket-based FIX server (port 9878)

### Message Processing
- **FIXMessageDecoder**: Netty decoder for incoming FIX messages
- **FIXMessageEncoder**: Netty encoder for outgoing FIX messages
- **FIXMessageHandler**: Netty channel handler for message processing
- **FIXProtocolHandler**: Core FIX protocol message parsing and handling

### Session Management
- **SessionManager**: Manages all active FIX sessions
- **FIXSessionImpl**: Traditional implementation of FIX session
- **NettyFIXSession**: Netty-specific FIX session implementation
- **HeartbeatManager**: Handles FIX heartbeat mechanism
- **SessionTimeoutHandler**: Manages session timeouts

### Message Validation and Storage
- **FIXValidator**: Validates FIX message format and content
- **MessageStore**: Interface for message persistence
- **InMemoryMessageStore**: In-memory implementation for development
- **MessageStoreImpl**: Database-backed implementation for production

### Client Components
- **FIXClient**: Client interface for connecting to FIX servers
- **FIXClientImpl**: Traditional socket-based client implementation
- **NettyFIXClientExample**: Netty-based client example

## Heartbeat Rules and Connection Recovery Scenarios

### Heartbeat Timing Rules

| Event | Timing | Action | Class Responsible |
|-------|--------|--------|-------------------|
| **Normal Heartbeat** | Every `HeartBtInt` seconds | Send Heartbeat (35=0) | `HeartbeatManager` |
| **Test Request** | No message for 1.5 × `HeartBtInt` | Send Test Request (35=1) | `HeartbeatManager` |
| **Session Timeout** | No response for 2.0 × `HeartBtInt` | Disconnect session | `SessionTimeoutHandler` |
| **Activity Update** | Any message received | Reset heartbeat timer | `HeartbeatManager.updateHeartbeat()` |

### Connection Loss Scenarios and Recovery Actions

#### Scenario 1: Network Interruption (Temporary)

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant Network as Network
    participant Server as FIX Server
    
    Note over Client,Server: Normal operation
    Client->>Server: Business messages
    Server->>Client: Responses
    
    Note over Network: Network interruption
    Client-xServer: Connection lost
    
    Note over Server: Server detects connection loss
    Server->>Server: channelInactive() triggered
    Server->>Server: Cleanup session resources
    Server->>Server: Log disconnection event
    
    Note over Client: Client detects connection loss
    Client->>Client: onDisconnected() callback
    Client->>Client: Start reconnection logic
    
    loop Reconnection Attempts
        Client->>Network: Attempt reconnection
        alt Network restored
            Client->>Server: TCP connection established
            Client->>Server: Logon message (35=A)
            Server->>Client: Logon response (35=A)
            Note over Client,Server: Session restored
        else Network still down
            Client->>Client: Wait backoff period
        end
    end
```

#### Scenario 2: Heartbeat Timeout

```mermaid
sequenceDiagram
    participant Client as FIX Client
    participant Server as FIX Server
    participant HBManager as HeartbeatManager
    
    Note over Client,Server: Active session, HeartBtInt=30s
    
    Note over Client: Client stops sending messages
    
    Server->>Server: 45s elapsed (1.5 × 30s)
    HBManager->>Client: Test Request (35=1, TestReqID=TEST_123)
    
    alt Client responds
        Client->>Server: Heartbeat (35=0, TestReqID=TEST_123)
        HBManager->>HBManager: Clear pending test request
        Note over Client,Server: Session continues normally
    else Client doesn't respond
        Server->>Server: 60s elapsed (2.0 × 30s)
        HBManager->>Server: Trigger session timeout
        Server->>Server: Force disconnect client
        Server->>Server: Cleanup session resources
        Note over Server: Session terminated due to timeout
    end
```

#### Scenario 3: Application Crash and Restart

**Client Application Crash:**
```mermaid
graph TB
    A[Client App Crashes] --> B[TCP Connection Drops]
    B --> C[Server Detects channelInactive]
    C --> D[Server Cleanup Session]
    D --> E[Client App Restarts]
    E --> F[Client Reconnects]
    F --> G[New Session Establishment]
    G --> H{Sequence Number Gap?}
    H -->|Yes| I[Request Message Replay]
    H -->|No| J[Resume Normal Operation]
    I --> K[GapFillManager Processes]
    K --> L[Replay Missing Messages]
    L --> J
```

**Server Application Restart:**
```mermaid
graph TB
    A[Server Restarts] --> B[All Client Connections Lost]
    B --> C[Clients Detect Disconnection]
    C --> D[Clients Start Reconnection]
    D --> E[Server Accepts New Connections]
    E --> F[Session Re-establishment]
    F --> G[Load Previous Session State]
    G --> H{Message Store Available?}
    H -->|Yes| I[Restore Sequence Numbers]
    H -->|No| J[Reset Sequence Numbers]
    I --> K[Check for Message Gaps]
    J --> L[Fresh Session Start]
    K --> M[Process Gap Fill Requests]
    M --> L
```

### Connection Recovery Configuration

#### Client-Side Recovery Settings
```java
// FIXClientConfiguration settings
public class ReconnectionConfig {
    private boolean autoReconnect = true;
    private int maxReconnectAttempts = 10;
    private int initialReconnectDelay = 1000; // ms
    private int maxReconnectDelay = 30000; // ms
    private double backoffMultiplier = 2.0;
    private boolean resetSeqNumOnReconnect = false;
}
```

#### Server-Side Session Management
```java
// Session cleanup actions on disconnection
public void onClientDisconnect(String sessionId) {
    // 1. Remove from active sessions
    sessionManager.removeSession(sessionId);
    
    // 2. Stop heartbeat monitoring
    heartbeatManager.unregisterSession(sessionId);
    
    // 3. Stop timeout monitoring
    timeoutHandler.unregisterSession(sessionId);
    
    // 4. Update session state in store
    messageStore.updateSessionState(sessionId, SessionState.DISCONNECTED);
    
    // 5. Log disconnection event
    auditLogger.logDisconnection(sessionId, reason);
}
```

### Message Replay and Gap Fill Process

When a client reconnects after a disconnection, the following sequence number reconciliation occurs:

```mermaid
sequenceDiagram
    participant Client as Reconnecting Client
    participant Server as FIX Server
    participant Store as MessageStore
    participant GapFill as GapFillManager
    
    Client->>Server: Logon (35=A, MsgSeqNum=150)
    Server->>Store: Get last sequence for session
    Store->>Server: Last sent: 175, Last received: 149
    
    Note over Server: Gap detected: Client missing msgs 150-175
    
    Server->>Client: Logon Response (35=A, MsgSeqNum=176)
    Server->>GapFill: Request gap fill for msgs 150-175
    
    loop For each missing message
        GapFill->>Store: Retrieve message by sequence
        alt Message found and should be replayed
            GapFill->>Client: Resend message (43=Y, 122=original_time)
        else Message not found or admin message
            GapFill->>Client: Gap Fill (35=4, 123=N, 36=seq_num)
        end
    end
    
    Note over Client,Server: Gap fill complete, resume normal processing
```

### Error Handling During Recovery

| Error Condition | Server Action | Client Action | Recovery Method |
|----------------|---------------|---------------|-----------------|
| **Invalid Sequence Number** | Send Reject (35=3) | Reset sequence or disconnect | Manual intervention |
| **Duplicate Logon** | Send Logout (35=5) | Wait and retry | Exponential backoff |
| **Session Limit Exceeded** | Send Reject (35=3) | Queue connection | Wait for available slot |
| **Authentication Failed** | Send Logout (35=5) | Check credentials | Manual credential update |
| **Message Store Unavailable** | Send Reject (35=3) | Retry later | Wait for store recovery |

This architecture provides both high-performance Netty-based processing and traditional socket-based connectivity, with comprehensive session management, message validation, storage capabilities, and robust connection recovery mechanisms.