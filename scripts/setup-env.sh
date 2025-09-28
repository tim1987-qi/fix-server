#!/bin/bash

# Environment Setup Script for FIX Server
# This script sets up the Java environment correctly for macOS

echo "=== FIX Server Environment Setup ==="
echo ""

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected macOS"
    
    # Find JDK 8 installation
    if [ -d "/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk" ]; then
        JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home"
    else
        # Find any JDK 8
        JDK_PATH=$(find /Library/Java/JavaVirtualMachines -name "jdk1.8*" -type d | head -n 1)
        if [ -n "$JDK_PATH" ]; then
            JAVA_HOME="$JDK_PATH/Contents/Home"
        else
            echo "❌ No JDK 8 found!"
            echo ""
            echo "Please install Oracle JDK 8 or OpenJDK 8:"
            echo "1. Download from: https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html"
            echo "2. Or install via Homebrew: brew install openjdk@8"
            echo ""
            exit 1
        fi
    fi
    
    echo "✅ Found JDK at: $JAVA_HOME"
    
    # Export environment variables
    export JAVA_HOME="$JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    
    # Verify installation
    echo ""
    echo "=== Java Environment Verification ==="
    echo "JAVA_HOME: $JAVA_HOME"
    echo "Java version: $(java -version 2>&1 | head -n 1)"
    echo "Javac version: $(javac -version 2>&1)"
    
    # Create environment file for future use
    cat > .env << EOF
# FIX Server Environment Variables
export JAVA_HOME="$JAVA_HOME"
export PATH="\$JAVA_HOME/bin:\$PATH"

# Database Configuration (optional)
# export DB_PASSWORD="your_db_password"
# export ADMIN_PASSWORD="your_admin_password"

# TLS Configuration (optional)
# export TLS_KEYSTORE_PATH="/path/to/keystore.p12"
# export TLS_KEYSTORE_PASSWORD="keystore_password"
EOF
    
    echo ""
    echo "✅ Environment setup complete!"
    echo ""
    echo "To use this environment in your current shell, run:"
    echo "  source .env"
    echo ""
    echo "To make this permanent, add the following to your ~/.bash_profile or ~/.zshrc:"
    echo "  export JAVA_HOME=\"$JAVA_HOME\""
    echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    echo ""
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Detected Linux"
    echo "Please ensure you have OpenJDK 8 installed:"
    echo "  sudo apt-get install openjdk-8-jdk  # Ubuntu/Debian"
    echo "  sudo yum install java-1.8.0-openjdk-devel  # CentOS/RHEL"
    
else
    echo "Unsupported operating system: $OSTYPE"
    echo "Please manually set JAVA_HOME to your JDK 8 installation"
fi