# FIX Server

Enterprise-grade FIX (Financial Information eXchange) protocol server for financial trading environments with dual-architecture support and comprehensive message management capabilities.

## 🚀 Key Features

### Core FIX Protocol
- **Complete FIX 4.4 and 5.0 protocol implementation** with full message validation
- **Dual server architecture**: Traditional socket-based (port 9878) + High-performance Netty-based (port 9879)
- **Advanced message parsing** with custom FIX message codecs and validation
- **Session management** with heartbeat monitoring and timeout handling

### Performance & Scalability
- **High-performance Netty server** with non-blocking I/O and configurable thread pools
- **Concurrent session management** supporting 1000+ simultaneous connections
- **Event-driven architecture** for optimal resource utilization
- **Configurable buffer sizes** and connection pooling

### Message Management
- **Complete message persistence** with JPA/Hibernate integration
- **Message replay service** with gap fill management for sequence number recovery
- **Audit trail** with comprehensive logging of all FIX messages
- **Database migration support** with Flyway for schema management

### Enterprise Features
- **Enterprise-grade TLS security** with mutual authentication
- **Real-time monitoring** with Prometheus metrics and health checks
- **Regulatory compliance** with full audit trails and message archiving
- **Production-ready error handling** and connection recovery
- **Cross-platform support** with Windows and Unix runner scripts

## 📋 Prerequisites

- **Java 11+ JDK** (Oracle JDK or OpenJDK recommended)
- **Maven 3.6+** (or use included Maven wrapper)
- **Database**: H2 (development) / PostgreSQL 12+ (production)
- **Optional**: Docker and Docker Compose for containerized deployment

## 🚀 Quick Start

### 1. Environment Setup

```bash
# Set up Java environment (macOS/Linux)
./setup-env.sh && source .env

# Windows users: Ensure Java 11+ and Maven are in PATH
```

### 2. Build & Test

```bash
# Build the application
./mvnw clean compile

# Run comprehensive test suite
./mvnw test

# Package for deployment
./mvnw package
```

### 3. Run the FIX Server

```bash
# Start both servers (socket + Netty)
./run.sh

# Windows
run.bat

# Or using Maven directly
./mvnw spring-boot:run
```

**Server Endpoints:**
- **Traditional FIX Server**: `localhost:9878`
- **High-Performance Netty Server**: `localhost:9879`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`

### 4. Test with FIX Clients

```bash
# Test traditional socket client
./run-client.sh

# Test high-performance Netty client
./run-netty-client.sh

# Windows equivalents
run-client.bat
run-netty-client.bat
```

### 5. Database Setup

**Development (H2 - Default):**
- Automatic setup with in-memory database
- No additional configuration required

**Production (PostgreSQL):**
```sql
CREATE DATABASE fixserver;
CREATE USER fixserver WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE fixserver TO fixserver;
```

Update `application-prod.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fixserver
    username: fixserver
    password: ${DB_PASSWORD}
```

Run migrations:
```bash
./mvnw flyway:migrate -Dspring.profiles.active=prod
```

## ⚙️ Configuration

### Server Configuration

Configure via `application.yml` or environment variables:

**Core Settings:**
```yaml
fix:
  server:
    port: 9878                    # Traditional socket server port
    heartbeat-interval: 30        # Heartbeat interval in seconds
    session-timeout: 120          # Session timeout in seconds
    
netty:
  server:
    port: 9879                    # Netty server port
    boss-threads: 1               # Boss thread pool size
    worker-threads: 4             # Worker thread pool size
    buffer-size: 8192             # Buffer size for connections
```

**Database Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:fixserver    # H2 for development
    # url: jdbc:postgresql://localhost:5432/fixserver  # PostgreSQL for production
  jpa:
    hibernate:
      ddl-auto: validate          # Use Flyway for schema management
```

### Environment Variables

- `DB_PASSWORD`: Database password
- `ADMIN_PASSWORD`: Admin interface password  
- `TLS_KEYSTORE_PATH`: Path to TLS keystore
- `TLS_KEYSTORE_PASSWORD`: TLS keystore password
- `FIX_SERVER_PORT`: Override default FIX server port
- `NETTY_SERVER_PORT`: Override default Netty server port

### Application Profiles

- **`dev`**: Development profile (default) - H2 database, debug logging
- **`test`**: Testing profile - In-memory database, test configurations
- **`prod`**: Production profile - PostgreSQL, optimized settings, security enabled

**Activate profiles:**
```bash
# Development (default)
./mvnw spring-boot:run

# Production
./mvnw spring-boot:run -Dspring.profiles.active=prod

# Multiple profiles
./mvnw spring-boot:run -Dspring.profiles.active=prod,monitoring
```

## 🏗️ Architecture

### Dual Server Implementation

**Traditional Socket Server (Port 9878):**
- Thread-per-connection model
- Suitable for moderate load scenarios
- Full FIX protocol compliance
- Session state management

**High-Performance Netty Server (Port 9879):**
- Event-driven, non-blocking I/O
- Optimized for high-throughput scenarios
- Custom FIX message codecs
- Configurable thread pools

### Core Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   FIX Client    │────│   FIX Server     │────│  Message Store  │
│                 │    │                  │    │                 │
│ • Socket Client │    │ • Protocol       │    │ • Persistence   │
│ • Netty Client  │    │ • Session Mgmt   │    │ • Replay        │
│ • Message Types │    │ • Validation     │    │ • Audit Trail   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                       ┌──────────────────┐
                       │   Monitoring     │
                       │                  │
                       │ • Health Checks  │
                       │ • Metrics        │
                       │ • Logging        │
                       └──────────────────┘
```

## 🐳 Docker Deployment

### Build and Run

```bash
# Build Docker image
docker build -t fix-server .

# Run with Docker Compose (includes PostgreSQL)
docker-compose up -d

# View logs
docker-compose logs -f fix-server

# Stop services
docker-compose down
```

### Docker Configuration

The `docker-compose.yml` includes:
- FIX Server application
- PostgreSQL database
- Volume mounts for persistence
- Environment variable configuration

## 📊 Monitoring & Observability

### Health Checks
- **Endpoint**: `GET /actuator/health`
- **Database connectivity**: Automatic health checks
- **FIX server status**: Port availability and session counts

### Metrics (Prometheus)
- **Endpoint**: `GET /actuator/prometheus`
- **Session metrics**: Active sessions, connection counts
- **Message metrics**: Messages processed, validation errors
- **Performance metrics**: Response times, throughput

### Logging
- **Structured logging** with JSON format in production
- **FIX message logging** with configurable levels
- **Audit trail** for all financial messages
- **Error tracking** with stack traces and context

## 📚 Documentation

- **[FIX Client Guide](FIX_CLIENT_GUIDE.md)**: Complete client implementation guide
- **[FIX Client Summary](FIX_CLIENT_SUMMARY.md)**: Quick reference for client usage
- **[Server Developer Guide](FIX_SERVER_DEVELOPMENT_GUIDE.md)**: Server architecture and development
- **[Setup Guide](SETUP_GUIDE.md)**: Detailed environment setup instructions

## 🧪 Testing

### Test Coverage
- **Unit Tests**: Individual component testing
- **Integration Tests**: Database and server integration
- **FIX Protocol Tests**: Message validation and parsing
- **Performance Tests**: Load testing for both server implementations

### Run Tests
```bash
# All tests
./mvnw test

# Specific test categories
./mvnw test -Dtest="*Test"           # Unit tests
./mvnw test -Dtest="*IntegrationTest" # Integration tests

# With coverage report
./mvnw test jacoco:report
```

## 🔧 Development

### Project Structure
```
src/main/java/com/fixserver/
├── client/          # FIX client implementations
├── config/          # Spring configuration
├── core/            # Core FIX message handling
├── netty/           # Netty-based server implementation
├── protocol/        # FIX protocol validation and parsing
├── replay/          # Message replay and gap fill
├── server/          # Traditional socket server
├── session/         # Session management and heartbeat
└── store/           # Message persistence layer
```

### Building from Source
```bash
# Clone repository
git clone https://github.com/tim1987-qi/fix-server.git
cd fix-server

# Build and test
./mvnw clean install

# Run locally
./mvnw spring-boot:run
```

## 📄 License

Enterprise License - See LICENSE file for details.

---

## 🚀 Getting Started Checklist

- [ ] Install Java 11+ and Maven 3.6+
- [ ] Clone the repository
- [ ] Run `./mvnw clean install`
- [ ] Start the server with `./run.sh`
- [ ] Test with clients using `./run-client.sh` and `./run-netty-client.sh`
- [ ] Check health at `http://localhost:8080/actuator/health`
- [ ] Review documentation in the guides above

**Need help?** Check the [Setup Guide](SETUP_GUIDE.md) for detailed instructions.