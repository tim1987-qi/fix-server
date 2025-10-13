#!/bin/bash

# Simple script to test the FIX server by sending a FIX message
# The server should be running on localhost:9879

echo "=== Testing FIX Server ==="
echo "Server: localhost:9879"
echo

# Create a simple FIX logon message
# Format: 8=FIX.4.4|9=<length>|35=A|49=CLIENT1|56=SERVER1|34=1|52=<timestamp>|98=0|108=30|10=<checksum>|
# SOH character is represented as \x01

# Build the message body (everything after BodyLength and before Checksum)
TIMESTAMP=$(date -u +"%Y%m%d-%H:%M:%S")
BODY="35=A\x0149=CLIENT1\x0156=SERVER1\x0134=1\x0152=${TIMESTAMP}\x0198=0\x01108=30\x01"

# Calculate body length
BODY_LENGTH=$(echo -n -e "$BODY" | wc -c | tr -d ' ')

# Build message without checksum
MSG_WITHOUT_CHECKSUM="8=FIX.4.4\x019=${BODY_LENGTH}\x01${BODY}"

# Calculate checksum (sum of all bytes mod 256)
CHECKSUM=$(echo -n -e "$MSG_WITHOUT_CHECKSUM" | od -An -tu1 | tr -d '\n' | awk '{sum=0; for(i=1;i<=NF;i++) sum+=$i; print sum%256}')
CHECKSUM_PADDED=$(printf "%03d" $CHECKSUM)

# Complete message
COMPLETE_MSG="${MSG_WITHOUT_CHECKSUM}10=${CHECKSUM_PADDED}\x01"

echo "Sending FIX Logon message..."
echo "Message (with | for SOH): $(echo -e "$COMPLETE_MSG" | tr '\x01' '|')"
echo

# Try to send the message using nc (netcat)
if command -v nc &> /dev/null; then
    echo "Using netcat to send message..."
    echo -n -e "$COMPLETE_MSG" | nc -w 2 localhost 9879
    echo
    echo "Message sent!"
elif command -v telnet &> /dev/null; then
    echo "Netcat not available, trying telnet..."
    echo "Note: telnet may not work well for binary protocols"
else
    echo "Neither nc (netcat) nor telnet is available"
    echo "Please install netcat: brew install netcat"
    exit 1
fi

echo
echo "Check the server logs to see if the message was received"
