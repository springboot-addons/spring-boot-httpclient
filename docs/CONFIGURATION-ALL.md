# Configuration Parameters Reference

This document lists all configuration parameters for spring-boot-httpclient5 with their default values and descriptions.

## Base Configuration

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.user-agent` | `HttpClient5/SpringBoot` | User-Agent header sent with all requests |
| `spring.httpclient5.jmx.domain` | `${spring.application.name}` | JMX monitoring domain for metrics collection |

## HTTP/1.0 Configuration

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.http1.buffer-size` | `8192` | Size of internal buffer for HTTP communication |
| `spring.httpclient5.http1.waitForContinueTimeout` | `PT30S` | Timeout for waiting for CONTINUE response in HTTP/1.1 |

## Connection Pool Configuration

### Synchronous Pool (Blocking)

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.pool.max-conn-total` | `200` | Maximum total connections in the pool |
| `spring.httpclient5.pool.max-connper-route` | `20` | Maximum connections per route |
| `spring.httpclient5.pool.dns-resolver` | `system` | DNS resolver strategy (system, default) |
| `spring.httpclient5.pool.conn-pool-policy` | `LIFO` | Connection pooling policy (LIFO/FIFO) |
| `spring.httpclient5.pool.connection-time-to-live` | `PT300S` | Connection time-to-live value |
| `spring.httpclient5.pool.pool-concurrency-policy` | `STRICT` | Pool concurrency policy (STRICT/LAX) |
| `spring.httpclient5.pool.default-connection-config.connect-timeout` | `PT5S` | Default connection timeout |
| `spring.httpclient5.pool.default-connection-config.socket-timeout` | `PT30S` | Default socket timeout |
| `spring.httpclient5.pool.default-socket-config.so-timeout` | `PT60S` | Default socket timeout for SO_TIMEOUT |
| `spring.httpclient5.pool.host-config` | `{}` | Host-specific configurations |

### Asynchronous Pool (Non-blocking)

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.async-pool.max-connper-route` | `20` | Maximum connections per route |
| `spring.httpclient5.async-pool.dns-resolver` | `system` | DNS resolver strategy (system, default) |
| `spring.httpclient5.async-pool.conn-pool-policy` | `LIFO` | Connection pooling policy (LIFO/FIFO) |
| `spring.httpclient5.async-pool.connection-time-to-live` | `PT300S` | Connection time-to-live value |
| `spring.httpclient5.async-pool.pool-concurrency-policy` | `LAX` | Pool concurrency policy (STRICT/LAX) |
| `spring.httpclient5.async-pool.default-connection-config.connect-timeout` | `PT1S` | Default connection timeout |
| `spring.httpclient5.async-pool.default-connection-config.socket-timeout` | `PT30S` | Default socket timeout |
| `spring.httpclient5.async-pool.default-socket-config.so-timeout` | `PT60S` | Default socket timeout for SO_TIMEOUT |
| `spring.httpclient5.async-pool.host-config` | `{}` | Host-specific configurations |

## Request Configuration

### Default Request Configuration

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.request-config.[default].connection-keep-alive` | `PT60S` | Keep-alive timeout |
| `spring.httpclient5.request-config.[default].connection-request-timeout` | `PT5S` | Connection request timeout |
| `spring.httpclient5.request-config.[default].response-timeout` | `PT30S` | Response timeout |
| `spring.httpclient5.request-config.[default].retry-config.max-retries` | `0` | Maximum number of retries |
| `spring.httpclient5.request-config.[default].retry-config.retry-interval` | `PT1S` | Retry interval between attempts |
| `spring.httpclient5.request-config.[default].interceptors` | `{}` | Interceptor enable/disable settings |
| `spring.httpclient5.request-config.[default].custom-request-context` | `{}` | Custom context properties |
| `spring.httpclient5.request-config.[default].credentials` | `null` | Authentication credentials |
| `spring.httpclient5.request-config.[default].proxy` | `null` | Proxy server configuration |
| `spring.httpclient5.request-config.[default].error-management.circuit-name` | `null` | Circuit breaker name for error management |
| `spring.httpclient5.request-config.[default].error-management.broken-circuit-action` | `503` | HTTP status code when circuit is broken |
| `spring.httpclient5.request-config.[default].headers-propagation.enabled` | `false` | Enable header propagation in web starter |
| `spring.httpclient5.request-config.[default].headers-propagation.up` | `null` | Regex pattern for headers to propagate upstream |
| `spring.httpclient5.request-config.[default].headers-propagation.down` | `null` | Regex pattern for headers to propagate downstream |
| `spring.httpclient5.request-config.[default].headers-propagation.add` | `{}` | Headers to add during propagation |

## Resilience4j Integration

This client supports integration with Resilience4j for circuit breaker and rate limiting capabilities. The configuration references Resilience4j instances defined in the standard Spring Boot Resilience4j configuration.

### Configuration Reference

The Resilience4j configuration is defined using standard Spring Boot Resilience4j properties, not inline within the httpclient5 configuration:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        minimumNumberOfCalls: 2
        eventConsumerBufferSize: 10
        failureRateThreshold: 100
        registerHealthIndicator: true
        waitDurationInOpenState: 1000ms
      httpbin-org:
        minimumNumberOfCalls: 3
        slidingWindowSize: 5
        eventConsumerBufferSize: 10
        failureRateThreshold: 100
        registerHealthIndicator: true
        waitDurationInOpenState: 1000ms
        
  ratelimiter:
    instances:
      httpbin-org:
        limitForPeriod: 1
        timeoutDuration: 1s
        limitRefreshPeriod: 1000ms
        registerHealthIndicator: true
```

Then reference these in your httpclient5 configuration:

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        error-management:
          circuit-name: default
          broken-circuit-action: 503
```

For more information about Resilience4j, see the [official documentation](https://resilience4j.readme.io/).

## Configuration Pattern Matching

Request configurations use pattern matching based on `method + uri`:

- `[default]` - Default configuration applied to all requests
- `[GET https://api.example.com/.*]` - Matches GET requests to any path under api.example.com
- `[POST https://testhost/.*]` - Matches POST requests to testhost
- `[.* https://proxy.example.com/.*]` - Matches any method to proxy.example.com

## Starter-Specific Configuration

### spring-boot-starter-httpclient5-web

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.request-config.[default].headers-propagation.enabled` | `false` | Enable header propagation |
| `spring.httpclient5.request-config.[default].headers-propagation.up` | `null` | Regex pattern for headers to propagate upstream |
| `spring.httpclient5.request-config.[default].headers-propagation.down` | `null` | Regex pattern for headers to propagate downstream |
| `spring.httpclient5.request-config.[default].headers-propagation.add` | `{}` | Headers to add during propagation |

### spring-boot-starter-httpclient5-resilience4j

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.request-config.[default].error-management.circuit-name` | `null` | Circuit breaker name for error management |
| `spring.httpclient5.request-config.[default].error-management.broken-circuit-action` | `503` | HTTP status code when circuit is broken |

### spring-boot-starter-httpclient5-actuator

No specific configuration parameters beyond the base httpclient5 settings.

## Usage Notes

1. Configuration values can be overridden using Spring Boot's standard configuration mechanisms (application.yml, environment variables, etc.)
2. Host-specific configurations take precedence over default configurations
3. Pattern matching follows the order defined in the configuration file, with more specific patterns taking precedence
4. All timeout values support ISO 8601 duration format (PT1S, PT30S, etc.)
5. Authentication credentials can be specified as `username:password` or `PREEMPTIVE(username:password)`