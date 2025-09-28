#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# Change to the project root directory
cd "$SCRIPT_DIR/.."

# FIX Server Run Script
# This script provides different ways to run the FIX Server application

echo "=== FIX Server Startup Script ==="
echo ""

# Set up Java environment for macOS
setup_java_macos() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # Check if we have a proper JDK installed
        if [ -d "/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk" ]; then
            export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home"
            echo "✓ Set JAVA_HOME to: $JAVA_HOME"
        elif [ -d "/Library/Java/JavaVirtualMachines" ]; then
            # Find any JDK 8
            JDK_PATH=$(find /Library/Java/JavaVirtualMachines -name "jdk1.8*" -type d | head -n 1)
            if [ -n "$JDK_PATH" ]; then
                export JAVA_HOME="$JDK_PATH/Contents/Home"
                echo "✓ Set JAVA_HOME to: $JAVA_HOME"
            else
                echo "⚠ No JDK 8 found in /Library/Java/JavaVirtualMachines"
                echo "Please install Oracle JDK 8 or OpenJDK 8"
                return 1
            fi
        else
            echo "⚠ No JDK installation found"
            return 1
        fi
        
        # Update PATH to use the correct Java
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
    return 0
}

# Function to check if Maven is available
check_maven() {
    if command -v mvn &> /dev/null; then
        echo "✓ Maven found: $(mvn -version | head -n 1)"
        return 0
    elif [ -f "./mvnw" ]; then
        echo "✓ Maven wrapper found"
        return 0
    else
        echo "✗ Maven not found. Please install Maven or use the provided Maven wrapper."
        return 1
    fi
}

# Function to check Java version
check_java() {
    # Set up Java environment first
    setup_java_macos
    
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo "✓ Java found: $JAVA_VERSION"
        
        # Check if we have javac (compiler)
        if command -v javac &> /dev/null; then
            JAVAC_VERSION=$(javac -version 2>&1)
            echo "✓ Java compiler found: $JAVAC_VERSION"
            return 0
        else
            echo "✗ Java compiler (javac) not found. Please install JDK (not just JRE)."
            return 1
        fi
    else
        echo "✗ Java not found. Please install Java 8 JDK."
        return 1
    fi
}

# Function to compile the application
compile_app() {
    echo ""
    echo "=== Compiling Application ==="
    
    if [ -f "./mvnw" ]; then
        ./mvnw clean compile
    else
        mvn clean compile
    fi
    
    if [ $? -eq 0 ]; then
        echo "✓ Compilation successful"
        return 0
    else
        echo "✗ Compilation failed"
        return 1
    fi
}

# Function to run with Maven
run_with_maven() {
    echo ""
    echo "=== Running with Maven ==="
    
    if [ -f "./mvnw" ]; then
        ./mvnw spring-boot:run
    else
        mvn spring-boot:run
    fi
}

# Function to run with Java directly (after building JAR)
run_with_java() {
    echo ""
    echo "=== Building JAR and Running with Java ==="
    
    # Build the JAR
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi
    
    if [ $? -eq 0 ]; then
        # Find the JAR file
        JAR_FILE=$(find target -name "fix-server-*.jar" | head -n 1)
        
        if [ -f "$JAR_FILE" ]; then
            echo "✓ JAR built successfully: $JAR_FILE"
            echo "Starting application..."
            java -jar "$JAR_FILE"
        else
            echo "✗ JAR file not found in target directory"
            return 1
        fi
    else
        echo "✗ JAR build failed"
        return 1
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [option]"
    echo ""
    echo "Options:"
    echo "  maven    - Run with Maven (recommended for development)"
    echo "  java     - Build JAR and run with Java directly"
    echo "  compile  - Just compile the application"
    echo "  check    - Check prerequisites"
    echo "  help     - Show this help message"
    echo ""
    echo "If no option is provided, will run with Maven by default."
}

# Main execution
case "${1:-maven}" in
    "check")
        check_java
        check_maven
        ;;
    "compile")
        check_java && check_maven && compile_app
        ;;
    "maven")
        check_java && check_maven && compile_app && run_with_maven
        ;;
    "java")
        check_java && check_maven && run_with_java
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        echo "Unknown option: $1"
        show_usage
        exit 1
        ;;
esac