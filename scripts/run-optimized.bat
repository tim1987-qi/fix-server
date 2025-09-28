@echo off
REM FIX Server High-Performance Startup Script for Windows
REM This script starts the FIX server with optimized JVM parameters for maximum performance

echo Starting FIX Server with Performance Optimizations...

REM JVM Performance Parameters
set JVM_OPTS=

REM Memory Settings (adjust based on available RAM)
set JVM_OPTS=%JVM_OPTS% -Xms4g
set JVM_OPTS=%JVM_OPTS% -Xmx4g
set JVM_OPTS=%JVM_OPTS% -XX:NewRatio=1
set JVM_OPTS=%JVM_OPTS% -XX:MaxDirectMemorySize=2g

REM Garbage Collection Optimization (G1GC for low latency)
set JVM_OPTS=%JVM_OPTS% -XX:+UseG1GC
set JVM_OPTS=%JVM_OPTS% -XX:MaxGCPauseMillis=10
set JVM_OPTS=%JVM_OPTS% -XX:G1HeapRegionSize=16m
set JVM_OPTS=%JVM_OPTS% -XX:+G1UseAdaptiveIHOP
set JVM_OPTS=%JVM_OPTS% -XX:G1MixedGCCountTarget=8
set JVM_OPTS=%JVM_OPTS% -XX:+UseStringDeduplication

REM JIT Compiler Optimizations
set JVM_OPTS=%JVM_OPTS% -server
set JVM_OPTS=%JVM_OPTS% -XX:+TieredCompilation
set JVM_OPTS=%JVM_OPTS% -XX:+UseCompressedOops
set JVM_OPTS=%JVM_OPTS% -XX:+UseCompressedClassPointers
set JVM_OPTS=%JVM_OPTS% -XX:+OptimizeStringConcat
set JVM_OPTS=%JVM_OPTS% -XX:+UseFastAccessorMethods

REM Performance Optimizations
set JVM_OPTS=%JVM_OPTS% -XX:+AggressiveOpts
set JVM_OPTS=%JVM_OPTS% -XX:+UseBiasedLocking
set JVM_OPTS=%JVM_OPTS% -XX:+DoEscapeAnalysis
set JVM_OPTS=%JVM_OPTS% -XX:+EliminateAllocations
set JVM_OPTS=%JVM_OPTS% -XX:+UseFastJNIAccessors

REM Memory Management
set JVM_OPTS=%JVM_OPTS% -XX:+AlwaysPreTouch
set JVM_OPTS=%JVM_OPTS% -XX:+UseLargePages

REM Network and I/O Optimizations
set JVM_OPTS=%JVM_OPTS% -Djava.net.preferIPv4Stack=true
set JVM_OPTS=%JVM_OPTS% -Djava.awt.headless=true
set JVM_OPTS=%JVM_OPTS% -Dfile.encoding=UTF-8
set JVM_OPTS=%JVM_OPTS% -Dsun.nio.ch.bugLevel=
set JVM_OPTS=%JVM_OPTS% -Dsun.nio.useCanonicalPrefixCache=false

REM Security Optimizations
set JVM_OPTS=%JVM_OPTS% -Djava.security.egd=file:/dev/./urandom

REM Monitoring and Debugging (optional - remove in production for max performance)
set JVM_OPTS=%JVM_OPTS% -XX:+PrintGC
set JVM_OPTS=%JVM_OPTS% -XX:+PrintGCDetails
set JVM_OPTS=%JVM_OPTS% -XX:+PrintGCTimeStamps
set JVM_OPTS=%JVM_OPTS% -Xloggc:logs\gc.log
set JVM_OPTS=%JVM_OPTS% -XX:+UseGCLogFileRotation
set JVM_OPTS=%JVM_OPTS% -XX:NumberOfGCLogFiles=5
set JVM_OPTS=%JVM_OPTS% -XX:GCLogFileSize=10M

REM JFR (Java Flight Recorder) for production monitoring
set JVM_OPTS=%JVM_OPTS% -XX:+FlightRecorder
set JVM_OPTS=%JVM_OPTS% -XX:StartFlightRecording=duration=60s,filename=logs\fix-server.jfr

REM Application-specific optimizations
set APP_OPTS=
set APP_OPTS=%APP_OPTS% --spring.profiles.active=prod
set APP_OPTS=%APP_OPTS% --fix.server.performance.enabled=true
set APP_OPTS=%APP_OPTS% --fix.server.performance.use-optimized-parser=true
set APP_OPTS=%APP_OPTS% --fix.server.performance.use-async-storage=true
set APP_OPTS=%APP_OPTS% --logging.level.com.fixserver=INFO

REM Create logs directory
if not exist logs mkdir logs

REM Print configuration
echo === FIX Server Performance Configuration ===
echo JVM Options: %JVM_OPTS%
echo App Options: %APP_OPTS%
echo =============================================

REM Windows-specific optimizations
echo Applying Windows-specific optimizations...
echo - Set process priority to HIGH
echo - Disable Windows Defender real-time scanning for better performance
echo - Consider disabling Windows Update during trading hours
echo - Use Windows Server for production deployments

REM Start the application
echo Starting FIX Server...
java %JVM_OPTS% -jar target\fix-server-1.0.0-SNAPSHOT.jar %APP_OPTS%

echo FIX Server stopped.
pause