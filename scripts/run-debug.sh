#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# Change to the project root directory
cd "$SCRIPT_DIR/.."

# FIX Server Debug Mode Startup Script
# This script starts the FIX server with JVM remote debugging enabled

set -e

# Configuration
DEBUG_PORT=${DEBUG_PORT:-5005}
LOG_FILE="server-debug.log"
PID_FILE="server.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -i :$port >/dev/null 2>&1; then
        return 0  # Port is in use
    else
        return 1  # Port is free
    fi
}

# Function to stop existing server
stop_server() {
    print_status "Stopping existing FIX server processes..."
    
    # Kill by process name
    pkill -f "spring-boot:run" 2>/dev/null || true
    
    # Kill by PID file if exists
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            print_status "Stopped server with PID: $pid"
        fi
        rm -f "$PID_FILE"
    fi
    
    # Wait for processes to stop
    sleep 2
}

# Function to start server in debug mode
start_debug_server() {
    print_status "Starting FIX Server in Debug Mode..."
    print_status "Debug Port: $DEBUG_PORT"
    print_status "Log File: $LOG_FILE"
    
    # Check if debug port is available
    if check_port $DEBUG_PORT; then
        print_warning "Debug port $DEBUG_PORT is already in use"
        print_status "Checking if it's our server..."
        
        # Check if it's our Java process
        local java_pid=$(lsof -t -i :$DEBUG_PORT 2>/dev/null | head -1)
        if [ -n "$java_pid" ]; then
            local cmd=$(ps -p $java_pid -o comm= 2>/dev/null || echo "unknown")
            if [[ "$cmd" == *"java"* ]]; then
                print_warning "Java process already using debug port. Continuing..."
            else
                print_error "Non-Java process using debug port $DEBUG_PORT"
                print_error "Please free the port or use a different DEBUG_PORT"
                exit 1
            fi
        fi
    fi
    
    # Source environment if available
    if [ -f ".env" ]; then
        print_status "Loading environment from .env file..."
        source .env
    fi
    
    # Set Maven options for debugging
    export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
    
    # Start the server
    print_status "Executing: MAVEN_OPTS=\"$MAVEN_OPTS\" ./mvnw spring-boot:run"
    
    nohup ./mvnw spring-boot:run \
        -Dspring-boot.run.jvmArguments="-Dlogging.level.com.fixserver=DEBUG -Dlogging.level.io.netty=DEBUG -Dspring.profiles.active=debug" \
        > "$LOG_FILE" 2>&1 &
    
    local server_pid=$!
    echo $server_pid > "$PID_FILE"
    
    print_status "Server starting with PID: $server_pid"
    print_status "Waiting for server to start..."
    
    # Wait for server to start
    local max_wait=30
    local wait_count=0
    
    while [ $wait_count -lt $max_wait ]; do
        if grep -q "Started FIXServerApplication" "$LOG_FILE" 2>/dev/null; then
            break
        fi
        
        if grep -q "BUILD FAILURE" "$LOG_FILE" 2>/dev/null; then
            print_error "Server failed to start. Check $LOG_FILE for details."
            exit 1
        fi
        
        sleep 1
        wait_count=$((wait_count + 1))
        echo -n "."
    done
    echo
    
    if [ $wait_count -ge $max_wait ]; then
        print_error "Server startup timeout. Check $LOG_FILE for details."
        exit 1
    fi
}

# Function to show server status
show_status() {
    print_success "=== FIX Server Debug Mode Status ==="
    echo
    
    # Check if debug port is listening
    if check_port $DEBUG_PORT; then
        print_success "✅ JVM Debug Port: $DEBUG_PORT (ACTIVE)"
        
        # Show connected debuggers
        local connections=$(lsof -i :$DEBUG_PORT 2>/dev/null | grep -v COMMAND | wc -l)
        if [ $connections -gt 1 ]; then
            print_success "✅ IDE Debugger: Connected"
        else
            print_warning "⚠️  IDE Debugger: Not connected"
        fi
    else
        print_error "❌ JVM Debug Port: $DEBUG_PORT (NOT ACTIVE)"
    fi
    
    # Check application ports
    local ports=("9878:Traditional FIX Server" "9879:Netty FIX Server" "8080:Web Management")
    
    for port_info in "${ports[@]}"; do
        local port=$(echo $port_info | cut -d: -f1)
        local name=$(echo $port_info | cut -d: -f2)
        
        if check_port $port; then
            print_success "✅ $name: Port $port (ACTIVE)"
        else
            print_error "❌ $name: Port $port (NOT ACTIVE)"
        fi
    done
    
    echo
    print_status "Debug Features:"
    print_status "  • Application Debug Logging: DEBUG level"
    print_status "  • Netty Debug Logging: Enabled"
    print_status "  • JVM Remote Debugging: Port $DEBUG_PORT"
    print_status "  • Debug Log File: $LOG_FILE"
    
    echo
    print_status "IDE Debug Connection:"
    print_status "  • Host: localhost"
    print_status "  • Port: $DEBUG_PORT"
    print_status "  • Transport: dt_socket"
    
    echo
    print_status "Useful Commands:"
    print_status "  • Monitor logs: tail -f $LOG_FILE"
    print_status "  • Stop server: ./run-debug.sh stop"
    print_status "  • Restart server: ./run-debug.sh restart"
}

# Function to show logs
show_logs() {
    if [ -f "$LOG_FILE" ]; then
        print_status "Showing last 50 lines of $LOG_FILE (Press Ctrl+C to exit tail mode):"
        echo
        tail -50 "$LOG_FILE"
        echo
        print_status "To follow logs in real-time: tail -f $LOG_FILE"
    else
        print_error "Log file $LOG_FILE not found"
    fi
}

# Main script logic
case "${1:-start}" in
    start)
        stop_server
        start_debug_server
        sleep 3
        show_status
        ;;
    stop)
        stop_server
        print_success "FIX Server stopped"
        ;;
    restart)
        stop_server
        start_debug_server
        sleep 3
        show_status
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs}"
        echo
        echo "Commands:"
        echo "  start   - Start server in debug mode (default)"
        echo "  stop    - Stop the server"
        echo "  restart - Restart the server"
        echo "  status  - Show server status"
        echo "  logs    - Show recent logs"
        echo
        echo "Environment Variables:"
        echo "  DEBUG_PORT - JVM debug port (default: 5005)"
        echo
        echo "Examples:"
        echo "  $0                    # Start in debug mode"
        echo "  $0 start              # Start in debug mode"
        echo "  DEBUG_PORT=5006 $0    # Start with custom debug port"
        echo "  $0 status             # Check server status"
        echo "  $0 logs               # View recent logs"
        exit 1
        ;;
esac