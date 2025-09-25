#!/bin/bash

# Script to run the Netty FIX Client Example
# Usage: ./run-netty-client.sh [host] [port] [senderCompId] [targetCompId]

# Default values
HOST=${1:-localhost}
PORT=${2:-9879}
SENDER_COMP_ID=${3:-CLIENT1}
TARGET_COMP_ID=${4:-SERVER1}

echo "Starting Netty FIX Client..."
echo "Host: $HOST"
echo "Port: $PORT"
echo "Sender Comp ID: $SENDER_COMP_ID"
echo "Target Comp ID: $TARGET_COMP_ID"
echo ""

# Source environment if available
if [ -f ".env" ]; then
    source .env
fi

# Build classpath
CLASSPATH="target/classes"
if [ -f "target/dependency-jars.txt" ]; then
    CLASSPATH="$CLASSPATH:$(cat target/dependency-jars.txt | tr '\n' ':')"
else
    # Build classpath using Maven
    MAVEN_CLASSPATH=$(./mvnw dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q 2>/dev/null)
    if [ $? -eq 0 ]; then
        CLASSPATH="$CLASSPATH:$MAVEN_CLASSPATH"
    else
        echo "Warning: Could not build Maven classpath. Some dependencies might be missing."
    fi
fi

# Run the Netty client
java -cp "$CLASSPATH" com.fixserver.netty.NettyFIXClientExample "$HOST" "$PORT" "$SENDER_COMP_ID" "$TARGET_COMP_ID"