# spring-boot-httpclient5 Configuration System Summary

## Overview

This project provides a Spring Boot starter for Apache HttpClient 5 with comprehensive configuration capabilities. The configuration system allows fine-grained control over HTTP client behavior through Spring Boot's configuration mechanisms.

## Key Configuration Components

### 1. Base Configuration Properties
- `user-agent`: Sets the User-Agent header for all requests
- `jmx.domain`: Configures JMX monitoring domain for metrics collection

### 2. HTTP/1.0 Specific Configuration
- `http1.buffer-size`: Size of internal buffer for HTTP communication
- `http1.waitForContinueTimeout`: Timeout for waiting for CONTINUE response in HTTP/1.1

### 3. Connection Pool Configuration

#### Synchronous Pool (Blocking)
- `pool.max-conn-total`: Maximum total connections in the pool
- `pool.max-connper-route`: Maximum connections per route
- `pool.dns-resolver`: DNS resolver strategy
- `pool.conn-pool-policy`: Connection pooling policy (LIFO/FIFO)
- `pool.connection-time-to-live`: Connection time-to-live value
- `pool.pool-concurrency-policy`: Pool concurrency policy (STRICT/LAX)
- `pool.default-connection-config`: Default connection configuration
- `pool.default-socket-config`: Default socket configuration
- `pool.host-config`: Host-specific configurations

#### Asynchronous Pool (Non-blocking)
- Similar configuration options to synchronous pool but for async connections

### 4. Request Configuration System

The request configuration system uses pattern matching based on HTTP method and URI:

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':                    # Default configuration
        # ... default settings
      
      '[GET https://api.example.com/.*]':  # Pattern matching GET requests
        # ... specific settings
        
      '[POST https://testhost/.*]':        # Pattern matching POST requests
        # ... specific settings
```

#### Request Configuration Properties:
- `connection-keep-alive`: Keep-alive timeout
- `connection-request-timeout`: Connection request timeout
- `response-timeout`: Response timeout
- `retry-config`: Retry configuration (max retries, retry interval)
- `interceptors`: Interceptor enable/disable settings
- `custom-request-context`: Custom context properties
- `credentials`: Authentication credentials (basic or preemptive)
- `proxy`: Proxy server configuration

## Configuration File Examples

### Complete Example
See `src/test/resources/application-example.yml` for a comprehensive example showing all configuration options.

### Minimal Example
```yaml
spring:
  httpclient5:
    user-agent: MyApplication/1.0
    pool:
      max-conn-total: 128
      max-connper-route: 30
    async-pool:
      max-connper-route: 25
```

## Pattern Matching Logic

Request configurations are matched using pattern matching of `method + uri`:

1. **Default**: `[default]` - Applied to all requests
2. **Method + URI**: `[GET https://api.example.com/.*]` - Matches GET requests to specific host
3. **Method + Host**: `[POST https://testhost/.*]` - Matches POST requests to specific host

## Configuration Loading Process

1. Spring Boot loads configuration from application.yml/application.properties
2. Configuration is processed by `HttpClient5Config` class
3. Connection managers and clients are built based on configurations
4. Request configurations are compiled into pattern matching rules
5. During request execution, appropriate configurations are applied automatically

## Testing

The configuration system includes:
- Unit tests in `HttpClient5ConfigTest.java`
- Integration tests in `HttpClient5ConfigIntegrationTest.java`
- Comprehensive test configuration in `application-example.yml`

## Usage

### In Services:
```java
@Service
public class MyService {
    private final HttpClient httpClient;
    
    public MyService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
```

The client automatically applies appropriate configurations based on request patterns, making it easy to have different behaviors for different endpoints without complex code changes.

## Documentation

Additional documentation includes:
- `README.md` - Overview and usage
- `docs/configuration-guide.md` - Detailed configuration guide
- `docs/usage-example.md` - Usage examples