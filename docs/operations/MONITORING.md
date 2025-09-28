# FIX Server Monitoring Guide

## 📊 Overview

The FIX Server provides comprehensive monitoring capabilities through Spring Boot Actuator, Prometheus metrics, and custom performance indicators.

## 🔍 Health Checks

### Basic Health Check

```bash
# Check overall server health
curl http://localhost:8080/actuator/health

# Expected response
{
  "status": "UP",
  "components": {
    "fixServer": {"status": "UP"},
    "nettyServer": {"status": "UP"},
    "asyncMessageStore": {"status": "UP"}
  }
}
```

### Detailed Health Information

```bash
# Get detailed health information
curl http://localhost:8080/actuator/health?showDetails=true
```

## 📈 Metrics Collection

### Available Metrics

#### Server Metrics
- `fix.server.connections.active` - Active client connections
- `fix.server.connections.total` - Total connections since startup
- `fix.server.sessions.active` - Active FIX sessions
- `fix.server.sessions.total` - Total sessions created

#### Message Processing Metrics
- `fix.server.messages.received` - Total messages received
- `fix.server.messages.sent` - Total messages sent
- `fix.server.messages.processing.time` - Message processing latency
- `fix.server.messages.parsing.time` - Message parsing time
- `fix.server.messages.formatting.time` - Message formatting time

#### Performance Metrics
- `fix.server.performance.parsing.throughput` - Messages parsed per second
- `fix.server.performance.formatting.throughput` - Messages formatted per second
- `fix.server.performance.memory.allocation` - Memory allocation rate
- `fix.server.performance.gc.pause.time` - Garbage collection pause times

### Accessing Metrics

```bash
# List all available metrics
curl http://localhost:8080/actuator/metrics

# Get specific metric
curl http://localhost:8080/actuator/metrics/fix.server.messages.processing.time

# Get JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## 🎯 Prometheus Integration

### Prometheus Endpoint

```bash
# Access Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Sample Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'fix-server'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
```

### Key Prometheus Metrics

```promql
# Message processing rate
rate(fix_server_messages_received_total[5m])

# Average processing latency
rate(fix_server_messages_processing_time_seconds_sum[5m]) / 
rate(fix_server_messages_processing_time_seconds_count[5m])

# Active connections
fix_server_connections_active

# Memory usage
jvm_memory_used_bytes{area="heap"}
```

## 📊 Performance Monitoring

### Real-time Performance Dashboard

Create a monitoring dashboard with these key indicators:

#### Latency Metrics
- Message parsing time (target: <100μs)
- Message processing time (target: <1ms)
- End-to-end latency (target: <5ms)

#### Throughput Metrics
- Messages per second (target: >25,000)
- Connection rate (new connections/sec)
- Session establishment rate

#### Resource Utilization
- CPU usage (target: <80%)
- Memory usage (heap and direct)
- GC pause times (target: <10ms)
- Network I/O rates

### Performance Alerts

Set up alerts for:

```promql
# High latency alert
fix_server_messages_processing_time_seconds{quantile="0.95"} > 0.005

# Low throughput alert
rate(fix_server_messages_received_total[5m]) < 1000

# High memory usage alert
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8

# GC pause time alert
rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m]) > 0.01
```

## 🔧 Custom Monitoring

### Application-Specific Metrics

The server exposes custom metrics for FIX-specific monitoring:

```java
// Custom metrics in the application
@Component
public class FIXServerMetrics {
    
    @EventListener
    public void onMessageReceived(MessageReceivedEvent event) {
        messageCounter.increment();
        processingTimer.record(event.getProcessingTime());
    }
}
```

### Log-based Monitoring

#### Important Log Patterns

Monitor these log patterns for issues:

```bash
# Connection issues
grep "Connection refused\|Connection timeout" logs/fix-server.log

# Performance warnings
grep "Slow message processing\|High latency detected" logs/fix-server.log

# Error patterns
grep "ERROR\|FATAL" logs/fix-server.log

# Session management issues
grep "Session timeout\|Logon failed" logs/fix-server.log
```

#### Log Aggregation

For production environments, use log aggregation:

```yaml
# logback-spring.xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <logLevel/>
            <loggerName/>
            <message/>
            <mdc/>
        </providers>
    </encoder>
</appender>
```

## 🚨 Alerting and Notifications

### Critical Alerts

Set up immediate alerts for:

1. **Server Down**: Health check failures
2. **High Error Rate**: >5% error rate in message processing
3. **Memory Leak**: Continuous memory growth
4. **Connection Limit**: >90% of max connections used
5. **Disk Space**: <10% free space for message storage

### Warning Alerts

Set up warning alerts for:

1. **Performance Degradation**: Latency >2x normal
2. **High CPU**: >80% CPU usage for >5 minutes
3. **GC Pressure**: GC pause times >50ms
4. **Connection Growth**: Rapid connection increase

### Sample Alert Configuration

```yaml
# AlertManager configuration
groups:
- name: fix-server
  rules:
  - alert: FIXServerDown
    expr: up{job="fix-server"} == 0
    for: 30s
    labels:
      severity: critical
    annotations:
      summary: "FIX Server is down"
      
  - alert: HighLatency
    expr: fix_server_messages_processing_time_seconds{quantile="0.95"} > 0.005
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High message processing latency detected"
```

## 📱 Monitoring Tools Integration

### Grafana Dashboard

Import the provided Grafana dashboard for comprehensive monitoring:

```json
{
  "dashboard": {
    "title": "FIX Server Monitoring",
    "panels": [
      {
        "title": "Message Processing Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(fix_server_messages_received_total[5m])"
          }
        ]
      }
    ]
  }
}
```

### New Relic Integration

```yaml
# newrelic.yml
common: &default_settings
  license_key: '<your-license-key>'
  app_name: FIX Server
  
production:
  <<: *default_settings
  monitor_mode: true
```

### DataDog Integration

```yaml
# datadog.yaml
api_key: <your-api-key>
logs_enabled: true
process_config:
  enabled: "true"
```

## 🔍 Troubleshooting Monitoring Issues

### Common Issues

1. **Metrics Not Available**
   - Check actuator endpoints are enabled
   - Verify security configuration allows access
   - Ensure micrometer dependencies are included

2. **High Cardinality Metrics**
   - Avoid using session IDs or message IDs as metric tags
   - Use bounded tag values
   - Implement metric sampling for high-volume metrics

3. **Performance Impact**
   - Monitor the monitoring overhead
   - Use async metric collection
   - Sample high-frequency metrics

### Debugging Monitoring

```bash
# Check actuator endpoints
curl http://localhost:8080/actuator

# Verify Prometheus format
curl http://localhost:8080/actuator/prometheus | head -20

# Check metric registry
curl http://localhost:8080/actuator/metrics | jq '.names[]' | grep fix
```

## 📚 Additional Resources

- [Performance Guide](../performance/PERFORMANCE_GUIDE.md) - Performance optimization
- [Debug Guide](DEBUG_GUIDE.md) - Debugging and troubleshooting
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Documentation](https://prometheus.io/docs/)