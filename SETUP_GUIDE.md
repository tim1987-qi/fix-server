# FIX Server Setup Guide

## Problem Resolution Summary

The original error "找不到或无法加载主类 FIXServerApplication" (Cannot find or load main class FIXServerApplication) was caused by:

1. **Missing JAVA_HOME environment variable**
2. **Maven using JRE instead of JDK** (Java browser plugin vs proper JDK)
3. **Missing TestContainers dependency versions**
4. **Incorrect main class configuration**

## ✅ Solutions Applied

### 1. Java Environment Setup
- Created `setup-env.sh` script to detect and configure proper JDK
- Set `JAVA_HOME` to point to actual JDK installation
- Updated `PATH` to use JDK binaries instead of browser plugin

### 2. Maven Configuration Fixes
- Added explicit main class configuration in `pom.xml`
- Fixed missing TestContainers versions
- Added proper source directory configuration
- Enhanced build plugins with correct versions

### 3. Project Structure Improvements
- Added Maven wrapper for consistent builds
- Created run scripts for different platforms (`run.sh`, `run.bat`)
- Added Docker support with multi-stage builds
- Included monitoring configuration (Prometheus, Grafana)

## 🚀 How to Run the Application

### Option 1: Using the Setup Script (Recommended)

```bash
# 1. Set up Java environment
./setup-env.sh

# 2. Load environment variables
source .env

# 3. Run the application
./run.sh
```

### Option 2: Manual Setup

```bash
# 1. Set JAVA_HOME manually (macOS example)
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Compile and run
./mvnw clean compile
./mvnw spring-boot:run
```

### Option 3: Using Docker

```bash
# Build and run with Docker
docker build -t fix-server .
docker run -p 8080:8080 -p 9878:9878 fix-server

# Or use Docker Compose for full stack
docker-compose up
```

## 📋 Verification Steps

### 1. Check Java Environment
```bash
echo $JAVA_HOME
java -version
javac -version
```

### 2. Test Compilation
```bash
./mvnw clean compile
```

### 3. Run Tests
```bash
./mvnw test
```

### 4. Check Application Health
Once running, visit:
- Health check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

## 🔧 Troubleshooting

### Issue: "No compiler is provided in this environment"
**Solution:** Ensure you're using JDK, not JRE
```bash
./setup-env.sh
source .env
```

### Issue: "Cannot find or load main class"
**Solution:** Use Maven to run the application
```bash
./mvnw spring-boot:run
```

### Issue: TestContainers version errors
**Solution:** Already fixed in `pom.xml` with explicit versions

### Issue: Maven wrapper not executable
**Solution:** Make it executable
```bash
chmod +x mvnw
```

## 📁 Project Structure

```
fix-server/
├── src/
│   ├── main/java/com/fixserver/     # Source code
│   ├── test/java/com/fixserver/     # Test code
│   └── main/resources/              # Configuration files
├── .mvn/wrapper/                    # Maven wrapper
├── docker-compose.yml              # Docker orchestration
├── Dockerfile                      # Container definition
├── pom.xml                         # Maven configuration
├── run.sh                          # Run script (Unix)
├── run.bat                         # Run script (Windows)
├── setup-env.sh                    # Environment setup
├── .env                            # Environment variables
└── README.md                       # Project documentation
```

## 🎯 Next Steps

1. **Development**: Use `./run.sh maven` for development
2. **Testing**: Run `./mvnw test` for unit tests
3. **Production**: Use Docker deployment with `docker-compose up`
4. **Monitoring**: Access Grafana at http://localhost:3000 (admin/admin)

## 📞 Support

If you encounter issues:

1. Check Java environment: `./setup-env.sh`
2. Verify Maven wrapper: `./mvnw --version`
3. Check logs in `logs/fix-server.log`
4. Review Docker logs: `docker-compose logs fix-server`

The application is now properly configured and ready for development and deployment!