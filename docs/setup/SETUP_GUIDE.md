# FIX Server Setup Guide

## 📋 Overview

This comprehensive guide covers the complete setup and configuration of the FIX Server for various environments, from development to production deployment.

## 🔧 System Requirements

### Minimum Requirements
- **Java**: OpenJDK 8 or Oracle JDK 8+
- **Memory**: 2GB RAM
- **Storage**: 1GB available disk space
- **Network**: TCP ports 8080, 9878, 9879 available

### Recommended Requirements
- **Java**: OpenJDK 11 or Oracle JDK 11+
- **Memory**: 4GB+ RAM (8GB+ for production)
- **Storage**: 10GB+ available disk space (SSD recommended)
- **Network**: Dedicated network interface for FIX traffic
- **CPU**: 4+ cores for high-throughput scenarios

### Production Requirements
- **Java**: OpenJDK 11+ with G1GC
- **Memory**: 8GB+ RAM (16GB+ for high-volume trading)
- **Storage**: 50GB+ SSD with backup strategy
- **Network**: Low-latency network infrastructure
- **CPU**: 8+ cores with NUMA optimization
- **OS**: Linux (Ubuntu 20.04+, CentOS 8+, or RHEL 8+)

## 🛠️ Installation

### 1. Java Installation

#### Ubuntu/Debian
```bash
# Install OpenJDK 11
sudo apt update
sudo apt install openjdk-11-jdk

# Verify installation
java -version
javac -version

# Set JAVA_HOME (add to ~/.bashrc)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin
```

#### CentOS/RHEL
```bash
# Install OpenJDK 11
sudo yum install java-11-openjdk-devel

# Verify installation
java -version

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
```

#### macOS
```bash
# Using Homebrew
brew install openjdk@11

# Add to PATH (add to ~/.zshrc or ~/.bash_profile)
export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@11"
```

#### Windows
1. Download OpenJDK 11 from [Adoptium](https://adoptium.net/)
2. Install using the MSI installer
3. Set JAVA_HOME environment variable
4. Add %JAVA_HOME%\bin to PATH

### 2. Maven Installation

#### Linux/macOS
```bash
# Download and install Maven
wget https://archive.apache.org/dist/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz
tar -xzf apache-maven-3.9.4-bin.tar.gz
sudo mv apache-maven-3.9.4 /opt/maven

# Set environment variables (add to ~/.bashrc)
export MAVEN_HOME=/opt/maven
export PATH=$PATH:$MAVEN_HOME/bin

# Verify installation
mvn -version
```

#### Windows
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to C:\Program Files\Apache\maven
3. Set MAVEN_HOME environment variable
4. Add %MAVEN_HOME%\bin to PATH

### 3. FIX Server Installation

#### From Source
```bash
# Clone the repository
git clone <repository-url>
cd fix-server

# Build the project
./mvnw clean compile

# Run tests to verify installation
./mvnw test

# Package the application
./mvnw package
```

#### Using Docker
```bash
# Build Docker image
docker build -t fix-server:latest .

# Run with Docker
docker run -p 8080:8080 -p 9878:9878 -p 9879:9879 fix-server:latest
```

#### Using Docker Compose
```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f fix-server
```

## ⚙️ Configuration

### 1. Basic Configuration

The main configuration file is `src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  shutdown: graceful

spring:
  application:
    name: fix-server
  security:
    user:
      name: admin
      password: ${ADMIN_PASSWORD:admin123}

fix:
  server:
    port: 9878
    netty:
      port: 9879
      boss-threads: 1
      worker-threads: 8
      max-connections: 10000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.fixserver: INFO
    root: WARN
```

### 2. Environment-Specific Configuration

#### Development Environment (`application-dev.yml`)
```yaml
spring:
  profiles:
    active: dev

logging:
  level:
    com.fixserver: DEBUG
    org.springframework: INFO

fix:
  server:
    netty:
      worker-threads: 2
      max-connections: 100
```

#### Production Environment (`application-prod.yml`)
```yaml
spring:
  profiles:
    active: prod

logging:
  level:
    com.fixserver: INFO
    root: WARN
  file:
    name: /var/log/fix-server/fix-server.log

fix:
  server:
    netty:
      worker-threads: 16
      max-connections: 50000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 3. Database Configuration (Optional)

For persistent storage, configure a database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fixserver
    username: fixserver
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

### 4. Security Configuration

#### TLS/SSL Configuration
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: fix-server
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
```

#### Authentication Configuration
```yaml
spring:
  security:
    user:
      name: ${ADMIN_USERNAME:admin}
      password: ${ADMIN_PASSWORD}
      roles: ADMIN

fix:
  security:
    authentication:
      enabled: true
      type: database  # or 'simple', 'ldap'
```

## 🚀 Running the Server

### 1. Development Mode
```bash
# Using Maven
./mvnw spring-boot:run

# Using scripts
./scripts/run.sh

# With specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Production Mode
```bash
# Build JAR
./mvnw clean package

# Run JAR
java -jar target/fix-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod

# With JVM optimizations
java -server -Xms4g -Xmx4g -XX:+UseG1GC \
     -jar target/fix-server-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=prod
```

### 3. As a Service (Linux)

Create a systemd service file `/etc/systemd/system/fix-server.service`:

```ini
[Unit]
Description=FIX Server
After=network.target

[Service]
Type=simple
User=fixserver
Group=fixserver
WorkingDirectory=/opt/fix-server
ExecStart=/usr/bin/java -server -Xms4g -Xmx4g -XX:+UseG1GC \
          -jar /opt/fix-server/fix-server.jar \
          --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable fix-server
sudo systemctl start fix-server
sudo systemctl status fix-server
```

## 🔍 Verification

### 1. Health Checks
```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Detailed health information
curl http://localhost:8080/actuator/health | jq '.'
```

### 2. Port Verification
```bash
# Check listening ports
netstat -tlnp | grep -E "(8080|9878|9879)"

# Alternative using ss
ss -tlnp | grep -E "(8080|9878|9879)"
```

### 3. Log Verification
```bash
# Check application logs
tail -f logs/fix-server.log

# Check system logs (if running as service)
sudo journalctl -u fix-server -f
```

### 4. Performance Verification
```bash
# Check metrics
curl http://localhost:8080/actuator/metrics

# Check Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep fix_server
```

## 🐳 Docker Deployment

### 1. Dockerfile
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/fix-server-*.jar fix-server.jar

EXPOSE 8080 9878 9879

ENTRYPOINT ["java", "-jar", "fix-server.jar"]
```

### 2. Docker Compose
```yaml
version: '3.8'

services:
  fix-server:
    build: .
    ports:
      - "8080:8080"
      - "9878:9878"
      - "9879:9879"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ADMIN_PASSWORD=secure_password
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped

  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=fixserver
      - POSTGRES_USER=fixserver
      - POSTGRES_PASSWORD=fixserver_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

## 🔧 Performance Tuning

### 1. JVM Tuning
```bash
# Production JVM settings
JAVA_OPTS="-server"
JAVA_OPTS="$JAVA_OPTS -Xms8g -Xmx8g"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=10"
JAVA_OPTS="$JAVA_OPTS -XX:+UseStringDeduplication"
JAVA_OPTS="$JAVA_OPTS -XX:+OptimizeStringConcat"
JAVA_OPTS="$JAVA_OPTS -XX:+UseFastAccessorMethods"
JAVA_OPTS="$JAVA_OPTS -XX:+UseCompressedOops"

java $JAVA_OPTS -jar fix-server.jar
```

### 2. Network Tuning (Linux)
```bash
# Increase network buffer sizes
echo 'net.core.rmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.core.wmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_rmem = 4096 87380 134217728' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_wmem = 4096 65536 134217728' >> /etc/sysctl.conf

# Apply changes
sudo sysctl -p
```

### 3. Application Tuning
```yaml
fix:
  server:
    netty:
      worker-threads: 16        # Match CPU cores
      max-connections: 50000    # Based on expected load
      buffer-size: 65536        # Optimize for message size
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Find process using port
lsof -i :9878
netstat -tlnp | grep 9878

# Kill process
kill -9 <PID>
```

#### 2. Java Version Issues
```bash
# Check Java version
java -version

# Update alternatives (Linux)
sudo update-alternatives --config java
```

#### 3. Memory Issues
```bash
# Check memory usage
free -h
top -p $(pgrep java)

# Adjust heap size
export JAVA_OPTS="-Xms2g -Xmx4g"
```

#### 4. Permission Issues
```bash
# Fix file permissions
chmod +x scripts/*.sh
chown -R fixserver:fixserver /opt/fix-server
```

## 📚 Next Steps

1. **[Client Integration](../client/CLIENT_GUIDE.md)** - Connect FIX clients
2. **[Performance Tuning](../performance/PERFORMANCE_GUIDE.md)** - Optimize for your workload
3. **[Monitoring Setup](../operations/MONITORING.md)** - Set up monitoring and alerting
4. **[Security Configuration](../operations/SECURITY.md)** - Secure your deployment
5. **[Deployment Guide](../operations/DEPLOYMENT.md)** - Production deployment strategies

## 🔗 Additional Resources

- **[Spring Boot Configuration Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)**
- **[Docker Documentation](https://docs.docker.com/)**
- **[Kubernetes Deployment Guide](../operations/DEPLOYMENT.md#kubernetes)**
- **[Performance Benchmarks](../performance/RESULTS.md)**