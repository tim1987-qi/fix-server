#!/bin/bash

# FIX Server High-Performance Startup Script
# This script starts the FIX server with optimized JVM parameters for maximum performance

echo "Starting FIX Server with Performance Optimizations..."

# JVM Performance Parameters
JVM_OPTS=""

# Memory Settings (adjust based on available RAM)
JVM_OPTS="$JVM_OPTS -Xms4g"                    # Initial heap size
JVM_OPTS="$JVM_OPTS -Xmx4g"                    # Maximum heap size (same as initial for consistency)
JVM_OPTS="$JVM_OPTS -XX:NewRatio=1"            # Young generation size (50% of heap)
JVM_OPTS="$JVM_OPTS -XX:MaxDirectMemorySize=2g" # Direct memory for Netty

# Garbage Collection Optimization (G1GC for low latency)
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"              # Use G1 garbage collector
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=10"   # Target max GC pause time
JVM_OPTS="$JVM_OPTS -XX:G1HeapRegionSize=16m"  # G1 region size
JVM_OPTS="$JVM_OPTS -XX:+G1UseAdaptiveIHOP"    # Adaptive IHOP
JVM_OPTS="$JVM_OPTS -XX:G1MixedGCCountTarget=8" # Mixed GC target
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication" # String deduplication

# JIT Compiler Optimizations
JVM_OPTS="$JVM_OPTS -server"                   # Server mode for better performance
JVM_OPTS="$JVM_OPTS -XX:+TieredCompilation"    # Enable tiered compilation
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"    # Compressed object pointers
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedClassPointers" # Compressed class pointers
JVM_OPTS="$JVM_OPTS -XX:+OptimizeStringConcat"  # Optimize string concatenation
JVM_OPTS="$JVM_OPTS -XX:+UseFastAccessorMethods" # Fast accessor methods

# Performance Optimizations
JVM_OPTS="$JVM_OPTS -XX:+AggressiveOpts"       # Enable aggressive optimizations
JVM_OPTS="$JVM_OPTS -XX:+UseBiasedLocking"     # Biased locking for better synchronization
JVM_OPTS="$JVM_OPTS -XX:+DoEscapeAnalysis"     # Escape analysis
JVM_OPTS="$JVM_OPTS -XX:+EliminateAllocations" # Eliminate allocations where possible
JVM_OPTS="$JVM_OPTS -XX:+UseFastJNIAccessors"  # Fast JNI accessors

# Memory Management
JVM_OPTS="$JVM_OPTS -XX:+AlwaysPreTouch"       # Pre-touch memory pages
JVM_OPTS="$JVM_OPTS -XX:+UseTransparentHugePages" # Use huge pages if available
JVM_OPTS="$JVM_OPTS -XX:+UseLargePages"        # Use large pages

# Network and I/O Optimizations
JVM_OPTS="$JVM_OPTS -Djava.net.preferIPv4Stack=true" # Prefer IPv4
JVM_OPTS="$JVM_OPTS -Djava.awt.headless=true"  # Headless mode
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"     # UTF-8 encoding
JVM_OPTS="$JVM_OPTS -Dsun.nio.ch.bugLevel="    # NIO optimizations
JVM_OPTS="$JVM_OPTS -Dsun.nio.useCanonicalPrefixCache=false"

# Security Optimizations
JVM_OPTS="$JVM_OPTS -Djava.security.egd=file:/dev/./urandom" # Faster random number generation

# Monitoring and Debugging (optional - remove in production for max performance)
JVM_OPTS="$JVM_OPTS -XX:+PrintGC"              # Print GC information
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"       # Detailed GC information
JVM_OPTS="$JVM_OPTS -XX:+PrintGCTimeStamps"    # GC timestamps
JVM_OPTS="$JVM_OPTS -Xloggc:logs/gc.log"       # GC log file
JVM_OPTS="$JVM_OPTS -XX:+UseGCLogFileRotation" # Rotate GC logs
JVM_OPTS="$JVM_OPTS -XX:NumberOfGCLogFiles=5"  # Number of GC log files
JVM_OPTS="$JVM_OPTS -XX:GCLogFileSize=10M"     # GC log file size

# JFR (Java Flight Recorder) for production monitoring
JVM_OPTS="$JVM_OPTS -XX:+FlightRecorder"       # Enable JFR
JVM_OPTS="$JVM_OPTS -XX:StartFlightRecording=duration=60s,filename=logs/fix-server.jfr" # 60s recording

# Application-specific optimizations
APP_OPTS=""
APP_OPTS="$APP_OPTS --spring.profiles.active=prod"
APP_OPTS="$APP_OPTS --fix.server.performance.enabled=true"
APP_OPTS="$APP_OPTS --fix.server.performance.use-optimized-parser=true"
APP_OPTS="$APP_OPTS --fix.server.performance.use-async-storage=true"
APP_OPTS="$APP_OPTS --logging.level.com.fixserver=INFO"

# System optimizations (requires root privileges)
echo "Applying system optimizations..."

# Network optimizations
if [ "$EUID" -eq 0 ]; then
    echo "Applying network optimizations (running as root)..."
    
    # TCP optimizations
    echo 'net.core.rmem_max = 134217728' >> /etc/sysctl.conf
    echo 'net.core.wmem_max = 134217728' >> /etc/sysctl.conf
    echo 'net.ipv4.tcp_rmem = 4096 87380 134217728' >> /etc/sysctl.conf
    echo 'net.ipv4.tcp_wmem = 4096 65536 134217728' >> /etc/sysctl.conf
    echo 'net.core.netdev_max_backlog = 5000' >> /etc/sysctl.conf
    echo 'net.ipv4.tcp_congestion_control = bbr' >> /etc/sysctl.conf
    
    sysctl -p
    
    # Set CPU governor to performance
    echo performance | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
    
    echo "System optimizations applied."
else
    echo "Not running as root - skipping system optimizations."
    echo "For maximum performance, run as root or apply these manually:"
    echo "  - Increase network buffer sizes"
    echo "  - Set CPU governor to 'performance'"
    echo "  - Enable TCP BBR congestion control"
fi

# Create logs directory
mkdir -p logs

# Print configuration
echo "=== FIX Server Performance Configuration ==="
echo "JVM Options: $JVM_OPTS"
echo "App Options: $APP_OPTS"
echo "============================================="

# Start the application
echo "Starting FIX Server..."
java $JVM_OPTS -jar target/fix-server-1.0.0-SNAPSHOT.jar $APP_OPTS

echo "FIX Server stopped."