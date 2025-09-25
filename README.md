# FIX Server

Enterprise-grade FIX (Financial Information eXchange) protocol server for financial trading environments.

## Features

- Complete FIX 4.4 and 5.0 protocol implementation
- High-performance concurrent session management (1000+ sessions)
- Enterprise-grade TLS security with mutual authentication
- Comprehensive message persistence and replay functionality
- Real-time monitoring with Prometheus metrics
- Regulatory compliance with full audit trails

## Prerequisites

- Java 8 JDK (not just JRE) - Oracle JDK or OpenJDK
- Maven 3.6 or higher (or use included Maven wrapper)
- PostgreSQL 12+ (for production)

## Quick Start

### 1. Set up Java environment (macOS)

```bash
# Run the environment setup script
./setup-env.sh

# Source the environment variables
source .env
```

### 2. Build the application

```bash
# Using Maven wrapper (recommended)
./mvnw clean compile

# Or using system Maven (if installed)
mvn clean compile
```

### 3. Run tests

```bash
./mvnw test
```

### 4. Run the application

```bash
# Using the run script (recommended)
./run.sh

# Or using Maven directly
./mvnw spring-boot:run
```

The server will start on port 8080 with the following endpoints:
- Health check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

### 4. Database Setup (Production)

For production deployment, set up PostgreSQL:

```sql
CREATE DATABASE fixserver;
CREATE USER fixserver WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE fixserver TO fixserver;
```

Then run database migrations:

```bash
mvn flyway:migrate
```

## Configuration

The application can be configured via `application.yml` or environment variables:

- `DB_PASSWORD`: Database password
- `ADMIN_PASSWORD`: Admin interface password
- `TLS_KEYSTORE_PATH`: Path to TLS keystore
- `TLS_KEYSTORE_PASSWORD`: TLS keystore password

## Profiles

- `dev`: Development profile (default)
- `test`: Testing profile
- `prod`: Production profile

Activate a profile with:
```bash
mvn spring-boot:run -Dspring.profiles.active=prod
```

## Docker

Build Docker image:
```bash
docker build -t fix-server .
```

Run with Docker Compose:
```bash
docker-compose up
```

## Monitoring

The application exposes Prometheus metrics at `/actuator/prometheus` and health checks at `/actuator/health`.

## License

Enterprise License - See LICENSE file for details.