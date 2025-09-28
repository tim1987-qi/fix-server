# FIX Server Debug Guide

This guide explains how to run and debug the FIX Server with full debugging capabilities.

## Quick Start

### Unix/macOS/Linux
```bash
# Start server in debug mode
./run-debug.sh

# Check status
./run-debug.sh status

# View logs
./run-debug.sh logs

# Stop server
./run-debug.sh stop
```

### Windows
```cmd
# Start server in debug mode
run-debug.bat

# Check status
run-debug.bat status

# View logs
run-debug.bat logs

# Stop server
run-debug.bat stop
```

## Debug Features

### 🔍 **JVM Remote Debugging**
- **Default Port**: 5005
- **Protocol**: JDWP (Java Debug Wire Protocol)
- **Transport**: dt_socket
- **Suspend**: No (server starts immediately)

### 📊 **Application Debug Logging**
- **FIX Server Components**: DEBUG level
- **Netty Framework**: DEBUG level
- **Spring Boot**: INFO level
- **Log File**: `server-debug.log`

### 🌐 **Server Ports**
- **Traditional FIX Server**: 9878
- **Netty FIX Server**: 9879
- **Web Management**: 8080
- **JVM Debug Port**: 5005

## IDE Configuration

### IntelliJ IDEA
1. Go to **Run** → **Edit Configurations**
2. Click **+** → **Remote JVM Debug**
3. Set configuration:
   - **Host**: `localhost`
   - **Port**: `5005`
   - **Command line args**: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`
4. Click **OK**
5. Start the FIX server with `./run-debug.sh`
6. Click **Debug** button in IDEA

### Eclipse
1. Go to **Run** → **Debug Configurations**
2. Right-click **Remote Java Application** → **New**
3. Set configuration:
   - **Project**: Select your FIX server project
   - **Connection Type**: Standard (Socket Attach)
   - **Host**: `localhost`
   - **Port**: `5005`
4. Click **Apply** and **Debug**

### VS Code
1. Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug FIX Server",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
```
2. Start the FIX server with `./run-debug.sh`
3. Press **F5** or go to **Run** → **Start Debugging**

## Advanced Configuration

### Custom Debug Port
```bash
# Unix/macOS/Linux
DEBUG_PORT=5006 ./run-debug.sh

# Windows
set DEBUG_PORT=5006
run-debug.bat
```

### Debug with Suspend
To start the server and wait for debugger attachment:
```bash
# Modify the script to use suspend=y
MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
```

### Memory Settings
Add JVM memory settings for debugging large applications:
```bash
# Add to JVM arguments
-Xms1g -Xmx2g -XX:+UseG1GC
```

## Debugging Workflow

### 1. **Start Debug Session**
```bash
./run-debug.sh start
```

### 2. **Verify Debug Connection**
```bash
./run-debug.sh status
```
Look for:
- ✅ JVM Debug Port: 5005 (ACTIVE)
- ✅ IDE Debugger: Connected

### 3. **Set Breakpoints**
- Open your IDE
- Set breakpoints in FIX server code
- Attach debugger to localhost:5005

### 4. **Test FIX Client Connection**
```bash
# Test with traditional client
./run-client.sh

# Test with Netty client  
./run-netty-client.sh
```

### 5. **Monitor Debug Logs**
```bash
# Follow logs in real-time
tail -f server-debug.log

# Filter for specific components
tail -f server-debug.log | grep "DEBUG.*FIX"
tail -f server-debug.log | grep "DEBUG.*Netty"
```

## Troubleshooting

### Debug Port Already in Use
```bash
# Check what's using the port
lsof -i :5005

# Kill process using the port
kill -9 $(lsof -t -i :5005)

# Or use different port
DEBUG_PORT=5006 ./run-debug.sh
```

### Server Won't Start
1. Check the log file: `cat server-debug.log`
2. Verify Java version: `java -version`
3. Check Maven: `./mvnw --version`
4. Ensure ports are free: `netstat -an | grep LISTEN`

### IDE Can't Connect
1. Verify server is running: `./run-debug.sh status`
2. Check debug port is listening: `lsof -i :5005`
3. Verify IDE debug configuration
4. Check firewall settings

### Performance Issues
1. Increase JVM memory: Add `-Xms1g -Xmx2g` to JVM args
2. Reduce log level: Change DEBUG to INFO for non-essential components
3. Disable Netty debug: Remove `-Dlogging.level.io.netty=DEBUG`

## Debug Log Analysis

### Key Log Patterns
```bash
# FIX message processing
grep "FIXMessage" server-debug.log

# Session management
grep "Session.*DEBUG" server-debug.log

# Netty events
grep "netty.*DEBUG" server-debug.log

# Connection events
grep -E "(Connected|Disconnected|Established)" server-debug.log

# Error analysis
grep -E "(ERROR|WARN)" server-debug.log
```

### Performance Monitoring
```bash
# JVM metrics
grep -E "(GC|Memory|Thread)" server-debug.log

# Connection metrics
grep -E "(Connection.*count|Session.*active)" server-debug.log

# Message throughput
grep -E "(Message.*processed|throughput)" server-debug.log
```

## Production Debugging

### Remote Debug (Caution!)
For production debugging (use with extreme caution):
```bash
# Enable debug on production server
MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Connect from local IDE
# Host: production-server-ip
# Port: 5005
```

**⚠️ Security Warning**: Never enable remote debugging on production without proper security measures (VPN, firewall rules, etc.)

### Log-Only Debugging
For production environments, use enhanced logging instead:
```bash
# Start with debug logging only (no remote debug)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.com.fixserver=DEBUG"
```

## Script Reference

### run-debug.sh / run-debug.bat Commands
- `start` - Start server in debug mode (default)
- `stop` - Stop the server gracefully
- `restart` - Stop and start the server
- `status` - Show comprehensive server status
- `logs` - Display recent log entries

### Environment Variables
- `DEBUG_PORT` - JVM debug port (default: 5005)
- `LOG_FILE` - Debug log file name (default: server-debug.log)

### Exit Codes
- `0` - Success
- `1` - General error (startup failure, port conflict, etc.)

## Best Practices

1. **Always use debug mode for development**
2. **Set meaningful breakpoints** - Focus on key FIX protocol handling
3. **Monitor memory usage** - Debug mode can increase memory consumption
4. **Use conditional breakpoints** - For high-frequency message processing
5. **Keep debug sessions short** - Debug mode impacts performance
6. **Review debug logs regularly** - Look for patterns and issues
7. **Test with realistic data** - Use production-like FIX message volumes

## Support

For issues with debugging:
1. Check this guide first
2. Review `server-debug.log` for errors
3. Verify IDE debug configuration
4. Test with minimal breakpoints
5. Check system resources (memory, CPU)

Happy debugging! 🐛🔍