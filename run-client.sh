#!/bin/bash

# FIX Client Runner Script
# Compiles and runs the FIX client example application

set -e

echo "=== FIX Client Runner Script ==="
echo

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed or not in PATH"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1-2)
echo "✓ Java version: $JAVA_VERSION"

# Set JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home"
        echo "✓ Set JAVA_HOME to: $JAVA_HOME"
    else
        echo "⚠️  JAVA_HOME not set, using system default"
    fi
fi

# Check if Maven wrapper exists
if [ ! -f "./mvnw" ]; then
    echo "❌ Maven wrapper not found"
    exit 1
fi

echo "✓ Maven wrapper found"
echo

# Parse command line arguments
HOST=${1:-localhost}
PORT=${2:-9876}
SENDER_COMP_ID=${3:-CLIENT1}
TARGET_COMP_ID=${4:-SERVER1}

echo "=== Client Configuration ==="
echo "Host: $HOST"
echo "Port: $PORT"
echo "Sender CompID: $SENDER_COMP_ID"
echo "Target CompID: $TARGET_COMP_ID"
echo

# Compile the project
echo "=== Compiling Project ==="
./mvnw clean compile -q
if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "❌ Compilation failed"
    exit 1
fi
echo

# Run the client
echo "=== Starting FIX Client ==="
echo "Press Ctrl+C to stop the client"
echo

# Use Maven exec plugin to run the client
./mvnw exec:java \
    -Dexec.mainClass="com.fixserver.client.FIXClientExample" \
    -Dexec.args="$HOST $PORT $SENDER_COMP_ID $TARGET_COMP_ID" \
    -q

echo
echo "FIX Client stopped"