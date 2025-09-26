# FIX Server - High-Performance Financial Trading Server

A comprehensive, production-ready Financial Information eXchange (FIX) protocol server implementation built with Spring Boot and Netty, featuring dual-server architecture, advanced performance optimization, and comprehensive monitoring capabilities.

## 🚀 Key Features

### **Dual Server Architecture**
- **Traditional Socket Server** (Port 9878): Thread-per-connection model for maximum compatibility
- **Netty Server** (Port 9879): High-performance, event-driven architecture for scalability and throughput
- **Seamless Integration**: Both servers share core FIX protocol handling and session management

### **FIX Protocol Compliance**
- **FIX 4.4 Protocol Support**: Complete implementation with comprehensive message validation
- **Session Management**: Full lifecycle management with heartbeat monitoring and timeout handling
- **Message Replay**: Gap fill and sequence number recovery for reliable message delivery
- **Connection Recovery**: Automatic reconnection with exponential backoff and session restoration

### **Performance & Monitoring**
- **Real-time Metrics**: Message throughput, processing latency, and connection statistics
- **Performance Optimization**: Built-in performance analyzer with optimization recommendations
- **Human-readable Logging**: FIX tag translation for improved debugging and monitoring
- **Debug Mode**: Comprehensive debugging with JVM remote debugging support

### **Storage & Persistence**
- **Flexible Storage**: In-memory for development, PostgreSQL for production
- **Message Audit**: Complete audit trail with message storage and replay capabilities
- **Session Persistence**: Session state recovery across server restarts

### **Client Libraries**
- **Multiple Client Types**: Traditional socket and Netty-based client implementations
- **Interactive Testing**: Command-line clients for manual testing and integration
- **Connection Handling**: Automatic reconnection and session recovery

## 📊 Performance Enhancements

### **Latest Optimizations (v2.0)**
- ✅ **FIX Tag Definitions**: Comprehensive tag library with human-readable field names
- ✅ **Performance Monitoring**: Real-time performance metrics and optimization recommendations
- ✅ **Memory Optimization**: Object pooling and garbage collection optimization
- ✅ **Improved Logging**: Human-readable FIX message logging with field name translation
- ✅ **Connection Pooling**: Optimized connection management and resource utilization

### **Performance Metrics**
- **Message Throughput**: Real-time messages per second tracking
- **Processing Latency**: Min/max/average processing time monitoring
- **Connection Statistics**: Active connections and peak concurrent usage
- **Memory Usage**: Heap utilization and GC performance tracking

## 🏗 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    FIX Server Application                   │
├─────────────────────────────────────────────────────────────┤
│  Traditional Server (9878)  │  Netty Server (9879)         │
│  ┌─────────────────────────┐ │ ┌─────────────────────────┐   │
│  │ Socket-based            │ │ │ Event-driven            │   │
│  │ Thread-per-connection   │ │ │ High-performance        │   │
│  └─────────────────────────┘ │ └─────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                 Core FIX Protocol Engine                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ Message     │ │ Session     │ │ Performance         │   │
│  │ Processing  │ │ Management  │ │ Monitoring          │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                    Storage Layer                           │
│  ┌─────────────────────────┐ ┌─────────────────────────┐   │
│  │ In-Memory Store         │ │ PostgreSQL Store        │   │
│  │ (Development)           │ │ (Production)            │   │
│  └─────────────────────────┘ └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- **Java 8+** (Java 11+ recommended for optimal performance)
- **Maven 3.6+**
- **PostgreSQL 12+** (optional, for production persistence)

### Installation & Setup

1. **Clone and Build**
```bash
git clone https://github.com/tim1987-qi/fix-server.git
cd fix-server
./mvnw clean compile
```

2. **Start in Development Mode**
```bash
./mvnw spring-boot:run
```

3. **Start in Debug Mode** (with JVM debugging on port 5005)
```bash
./run-debug.sh
```

### Server Endpoints
- **Traditional FIX Server**: `localhost:9878`
- **Netty FIX Server**: `localhost:9879`
- **Web Management**: `http://localhost:8080`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`
- **JVM Debug Port**: `localhost:5005` (debug mode only)

## 🧪 Testing & Client Usage

### **Interactive Clients**

**Traditional Socket Client:**
```bash
./run-client.sh
# Connects to port 9878 with interactive command interface
```

**High-Performance Netty Client:**
```bash
./run-netty-client.sh
# Connects to port 9879 with event-driven processing
```

### **Client Commands**
```
Commands available in interactive mode:
- market <symbol> <buy|sell> <quantity>     # Send market order
- limit <symbol> <buy|sell> <quantity> <price>  # Send limit order
- heartbeat                                 # Send heartbeat
- quit                                      # Exit client
```

### **Example Trading Session**
```bash
$ ./run-netty-client.sh
Connected to Netty FIX Server at localhost:9879
Logged on successfully

=== Netty FIX Client Interactive Session ===
> market AAPL buy 100
Order sent: AAPL BUY 100 shares (Market Order)
Received execution report: Order accepted

> limit MSFT sell 50 150.00
Order sent: MSFT SELL 50 shares @ $150.00 (Limit Order)
Received execution report: Order accepted

> quit
Disconnected from server
```

## 📈 Performance Monitoring

### **Real-time Performance Dashboard**
Access performance metrics at: `http://localhost:8080/actuator/prometheus`

### **Performance Statistics**
The server provides comprehensive performance monitoring:

```bash
# View performance summary in logs
tail -f server-debug.log | grep "Performance Summary"

=== FIX Server Performance Summary ===
Total Messages Processed: 15,847
Total Bytes Processed: 2.3 MB
Average Processing Time: 1.23 ms
Message Throughput: 2,450 msg/sec
Data Throughput: 0.35 MB/sec
Active Connections: 5
Max Concurrent Connections: 12
```

### **Optimization Recommendations**
The server automatically analyzes performance and provides recommendations:

```
=== Optimization Recommendations ===
- Performance looks good! No specific optimizations needed.
- Consider using Netty server for connections > 50 concurrent users
- Memory usage is optimal at 45.2%
```

## 🔧 Configuration

### **Application Configuration** (`application.yml`)
```yaml
fix:
  server:
    port: 9878                    # Traditional server port
    netty-port: 9879             # Netty server port
    max-sessions: 100            # Maximum concurrent sessions
    heartbeat-interval: 30       # Heartbeat interval in seconds
    
  performance:
    monitoring-enabled: true     # Enable performance monitoring
    metrics-interval: 60         # Metrics collection interval
    optimization-alerts: true   # Enable optimization recommendations

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fixserver
    username: fixuser
    password: fixpass
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false              # Set to true for SQL debugging
```

### **Debug Configuration**
```bash
# Start with custom debug port
DEBUG_PORT=5006 ./run-debug.sh

# Start with performance profiling
JAVA_OPTS="-XX:+UseG1GC -XX:+PrintGCDetails" ./run-debug.sh
```

## 🔍 Advanced Features

### **Heartbeat Management**
- **Automatic Heartbeats**: Configurable interval (default: 30 seconds)
- **Test Requests**: Sent after 1.5x heartbeat interval
- **Timeout Detection**: Session terminated after 2.0x interval
- **Connection Recovery**: Automatic reconnection with exponential backoff

### **Message Replay & Gap Fill**
- **Sequence Number Tracking**: Automatic gap detection
- **Message Replay**: Resend missing messages on reconnection
- **Gap Fill Messages**: Administrative message handling
- **Session Recovery**: State restoration across disconnections

### **Human-Readable Logging**
```bash
# Before: Raw FIX tags
8=FIX.4.4|9=71|35=D|49=CLIENT1|56=SERVER1|55=AAPL|54=1|38=100

# After: Human-readable format
BeginString=FIX.4.4 | BodyLength=71 | MsgType=D(NewOrderSingle) | 
SenderCompID=CLIENT1 | TargetCompID=SERVER1 | Symbol=AAPL | 
Side=1(Buy) | OrderQty=100
```

### **Performance Optimization**
- **Object Pooling**: Reduced garbage collection pressure
- **String Builder Reuse**: Optimized string operations
- **Connection Metrics**: Real-time connection monitoring
- **Memory Analysis**: Automatic memory usage optimization

## 📚 Documentation

### **Comprehensive Guides**
- **[Setup Guide](SETUP_GUIDE.md)**: Detailed installation and configuration
- **[Client Guide](FIX_CLIENT_GUIDE.md)**: Client library usage and examples
- **[Development Guide](FIX_SERVER_DEVELOPMENT_GUIDE.md)**: Development and contribution guidelines
- **[Debug Guide](DEBUG_GUIDE.md)**: Debugging and troubleshooting
- **[Architecture Flowchart](FIX_SERVER_FLOWCHART.md)**: Detailed system architecture and message flows

### **API Documentation**
- **FIX Protocol**: Full FIX 4.4 specification compliance
- **REST Endpoints**: Management and monitoring APIs
- **Client Libraries**: Java client SDK with examples

## 🛠 Development & Contribution

### **Development Setup**
```bash
# Clone repository
git clone https://github.com/tim1987-qi/fix-server.git
cd fix-server

# Setup development environment
./setup-env.sh

# Run tests
./mvnw test

# Start in debug mode
./run-debug.sh
```

### **Code Quality**
- **Test Coverage**: Comprehensive unit and integration tests
- **Code Style**: Consistent formatting and documentation
- **Performance Testing**: Load testing and benchmarking
- **Security**: Input validation and secure session management

## 📊 Recent Improvements (v2.0)

### **Performance Enhancements**
- ✅ **FIX Tag Library**: Comprehensive tag definitions with human-readable names
- ✅ **Performance Monitoring**: Real-time metrics and optimization recommendations
- ✅ **Memory Optimization**: Object pooling and GC optimization
- ✅ **Improved Logging**: Human-readable FIX message formatting
- ✅ **Debug Scripts**: Enhanced debugging with JVM remote debugging

### **Architecture Improvements**
- ✅ **Dual Server Support**: Traditional and Netty servers running simultaneously
- ✅ **Connection Recovery**: Robust reconnection with exponential backoff
- ✅ **Session Management**: Enhanced session lifecycle and state management
- ✅ **Message Replay**: Gap fill and sequence number recovery
- ✅ **Comprehensive Documentation**: Detailed flowcharts and guides

### **Developer Experience**
- ✅ **Interactive Clients**: Command-line clients for easy testing
- ✅ **Debug Mode**: JVM debugging with IDE integration
- ✅ **Performance Dashboard**: Real-time monitoring and recommendations
- ✅ **Human-readable Logs**: FIX tag translation for better debugging

## 🤝 Contributing

We welcome contributions! Please see our [Development Guide](FIX_SERVER_DEVELOPMENT_GUIDE.md) for details on:
- Code style and standards
- Testing requirements
- Pull request process
- Performance benchmarking

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check our comprehensive guides in the `/docs` folder
- **Issues**: Report bugs and feature requests on GitHub Issues
- **Performance**: Use the built-in performance analyzer for optimization guidance
- **Debug**: Enable debug mode for detailed troubleshooting

---

**Built with ❤️ for the financial trading community**

*High-performance FIX protocol server designed for modern trading systems*