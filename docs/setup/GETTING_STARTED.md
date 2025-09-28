# Getting Started with FIX Server

## 🚀 5-Minute Quick Start

This guide will get you up and running with the FIX Server in under 5 minutes, including testing both traditional and Netty-based server implementations.

## ✅ Prerequisites

- **Java 8+** (JDK 11+ recommended for optimal performance)
- **Maven 3.6+** (or use included Maven wrapper)
- **4GB+ RAM** recommended for performance testing
- **Terminal/Command Prompt** access

## 🛠️ Installation

### 1. Clone and Setup
```bash
# Clone the repository
git clone <repository-url>
cd fix-server

# Make scripts executable (Unix/macOS)
chmod +x scripts/*.sh

# Verify Java installation
java -version
```

### 2. Quick Build and Start
```bash
# Build the project
./mvnw clean compile

# Start the server
./mvnw spring-boot:run

# Alternative: Use the run script
./scripts/run.sh
```

### 3. Verify Server Status

The server starts with the following services:

| Service Type | Port | Description |
|-------------|------|-------------|
| **Traditional FIX** | 9878 | Blocking I/O, reliable |
| **Netty FIX** | 9879 | Non-blocking NIO, high-performance |
| **Management** | 8080 | Web interface & metrics |

**Verify all services are running:**
```bash
# Check listening ports
netstat -an | grep LISTEN | grep -E "(9878|9879|8080)"

# Check health status
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"fixServer":{"status":"UP"},"nettyServer":{"status":"UP"}}}
```

## 🧪 Test Client Connections

### Test 1: Traditional FIX Server (Port 9878)
```bash
# Connect to traditional server
./scripts/run-client.sh localhost 9878 CLIENT1 SERVER1
```

**Expected Output:**
```
=== FIX Client Runner Script ===
✓ Java version: 1.8
✓ Compilation successful
=== Starting FIX Client ===
Connected to FIX server
Sending logon message: 8=FIX.4.4|9=71|34=1|35=A|49=CLIENT1|52=2025-...
```

### Test 2: High-Performance Netty Server (Port 9879)
```bash
# Connect to Netty server (recommended for production)
./scripts/run-netty-client.sh localhost 9879 CLIENT1 SERVER1
```

**Expected Output:**
```
=== Netty FIX Client Runner Script ===
✓ Connected to Netty FIX server at localhost:9879
✓ Sent logon message: 8=FIX.4.4|9=71|34=1|35=A|49=CLIENT1|52=...
```

## ⚡ Performance Verification

### Check Performance Metrics
```bash
# View performance metrics
curl http://localhost:8080/actuator/metrics/fix.server.messages.processing.time

# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep fix_server
```

### Performance Indicators
The server delivers:

| Metric | Target | Typical Result |
|--------|--------|----------------|
| **Message Parsing** | <100μs | ~60μs (60-70% improvement) |
| **Throughput** | >25,000 msg/sec | >40,000 msg/sec |
| **Memory Efficiency** | 50% reduction | 80% reduction achieved |
| **Latency (P95)** | <1ms | <500μs |

## 🔧 Basic Configuration

### Default Configuration
The server comes pre-configured with optimal settings in `src/main/resources/application.yml`:

```yaml
server:
  port: 8080                    # Management interface port

fix:
  server:
    port: 9878                  # Traditional FIX server port
    netty:
      port: 9879                # Netty server port
      boss-threads: 1           # Connection acceptance threads
      worker-threads: 8         # I/O processing threads
      max-connections: 10000    # Maximum connections

spring:
  security:
    user:
      name: admin               # Management interface username
      password: admin123        # Change in production!

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### Quick Configuration Changes

**For Development:**
```yaml
logging:
  level:
    com.fixserver: DEBUG        # Enable debug logging
```

**For High-Throughput Production:**
```yaml
fix:
  server:
    netty:
      worker-threads: 16        # Increase for high load
      max-connections: 50000
```

## 🎯 Next Steps

### Immediate Next Steps
1. **[Setup Guide](SETUP_GUIDE.md)** - Detailed configuration options
2. **[Client Integration](../client/CLIENT_GUIDE.md)** - Integrate with your applications
3. **[Performance Tuning](../performance/PERFORMANCE_GUIDE.md)** - Optimize for your workload

### Development & Operations
4. **[Development Guide](../development/DEVELOPMENT_GUIDE.md)** - Architecture and development
5. **[Monitoring Setup](../operations/MONITORING.md)** - Production monitoring
6. **[Deployment Guide](../operations/DEPLOYMENT.md)** - Production deployment

## 🚨 Troubleshooting

### Common Issues & Solutions

#### Java Issues
```bash
# Issue: "Cannot find or load main class"
# Solution: Set JAVA_HOME
export JAVA_HOME="/path/to/your/jdk"
java -version

# Issue: "Unsupported Java version"
# Solution: Install Java 8+
sudo apt install openjdk-11-jdk  # Ubuntu/Debian
brew install openjdk@11          # macOS
```

#### Port Conflicts
```bash
# Issue: "Address already in use"
# Check what's using the ports
lsof -i :9878
lsof -i :9879
lsof -i :8080

# Kill conflicting processes or change ports in application.yml
```

#### Permission Issues
```bash
# Issue: "Permission denied" on scripts
# Solution: Make scripts executable
chmod +x scripts/*.sh

# Issue: Can't write to logs directory
# Solution: Create logs directory
mkdir -p logs
chmod 755 logs
```

#### Connection Issues
```bash
# Issue: Client can't connect
# Check server is running
curl http://localhost:8080/actuator/health

# Check firewall settings
sudo ufw status                   # Ubuntu
sudo firewall-cmd --list-ports    # CentOS/RHEL
```

### Getting Help
1. **Check Logs**: `tail -f logs/fix-server.log`
2. **Debug Mode**: Add `--debug` to startup command
3. **Health Check**: `curl http://localhost:8080/actuator/health`
4. **Metrics**: `curl http://localhost:8080/actuator/metrics`

## 🎉 Success Indicators

You've successfully set up the FIX Server when you see:

✅ **Server Started**: Both ports 9878 and 9879 are listening  
✅ **Health Check**: `/actuator/health` returns `{"status":"UP"}`  
✅ **Client Connection**: Test clients can connect and send logon messages  
✅ **Performance**: Metrics show sub-millisecond processing times  

**Congratulations! Your FIX Server is ready for development and testing.**

For production deployment, continue with the [Setup Guide](SETUP_GUIDE.md) and [Deployment Guide](../operations/DEPLOYMENT.md).