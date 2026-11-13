# Configuration Guide for spring-boot-httpclient5

## Overview

This guide explains how to configure the spring-boot-httpclient5 starter with various options for connection pooling, request handling, and HTTP behavior.

## Connection Pool Configuration

### Synchronous Pool (Blocking)

```yaml
spring:
  httpclient5:
    pool:
      max-conn-total: 256
      max-connper-route: 40
      dns-resolver: system
      conn-pool-policy: LIFO
      connection-time-to-live: PT180S
      pool-concurrency-policy: STRICT
      default-connection-config:
        connect-timeout: PT0.7S
        socket-timeout: PT45S
      default-socket-config:
        so-timeout: PT120S
      host-config:
        '[https://example.com]':
          connect-timeout: PT1S
          max-connections: 10
```

### Asynchronous Pool (Non-blocking)

```yaml
spring:
  httpclient5:
    async-pool:
      max-connper-route: 25
      dns-resolver: system
      conn-pool-policy: LIFO
      connection-time-to-live: PT180S
      pool-concurrency-policy: LAX
      default-connection-config:
        connect-timeout: PT0.2S
        socket-timeout: PT20S
      default-socket-config:
        so-timeout: PT300S
      host-config:
        '[https://testhost:4443]':
          connect-timeout: PT0.4S
          max-connections: 50
```

## Request Configuration

Request configurations are applied based on patterns matching HTTP method and URI:

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        connection-keep-alive: PT60S
        connection-request-timeout: PT2S
        response-timeout: PT5S
        retry-config:
          max-retries: 3
          retry-interval: PT5S
        interceptors:
          '[myinter]': false
        custom-request-context:
          '[propA]': "default-value"
        error-management:
          circuit-name: null
          broken-circuit-action: 503
      
      # Pattern matching for GET requests to a specific host
      '[GET https://api.example.com/.*]':
        response-timeout: 30000
        interceptors:
          '[myinter]': true
        custom-request-context:
          '[propA]': "surcharge"
      
      # Authentication configuration
      '[GET https://api.example.com/basic-auth/.*]':
        credentials: admin:pwd
      
      # Proxy configuration
      '[POST https://proxy.example.com/.*]':
        proxy: http://localhost:3128
```

## Pattern Matching

Request configurations use pattern matching based on `method + uri`:

- `[GET https://api.example.com/.*]` - Matches GET requests to any path under api.example.com
- `[POST https://testhost/.*]` - Matches POST requests to testhost
- `[default]` - Default configuration applied to all requests

## Starter-Specific Configuration

### spring-boot-starter-httpclient5-web

This starter provides additional features for web applications including header propagation capabilities.

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        headers-propagation:
          enabled: true
          up: X-TEST-.*
          down: X-TEST-.*
          add:
            '[X-TU]': "SRU ADDED HEADER"
```

### spring-boot-starter-httpclient5-resilience4j

This starter integrates with Resilience4j for circuit breaker and rate limiting capabilities.

```yaml
# Define Resilience4j instances in standard Spring Boot configuration
resilience4j:
  circuitbreaker:
    instances:
      default:
        minimumNumberOfCalls: 2
        eventConsumerBufferSize: 10
        failureRateThreshold: 100
        registerHealthIndicator: true
        waitDurationInOpenState: 1000ms

# Reference Resilience4j instances in httpclient5 configuration
spring:
  httpclient5:
    request-config:
      '[default]':
        error-management:
          circuit-name: default
          broken-circuit-action: 503
```

### spring-boot-starter-httpclient5-actuator

This starter provides Actuator integration for monitoring and management capabilities.

## Authentication Configuration

### Basic Authentication

```yaml
spring:
  httpclient5:
    request-config:
      '[GET https://api.example.com/basic-auth/.*]':
        credentials: admin:pwd
```

### Preemptive Authentication

```yaml
spring:
  httpclient5:
    request-config:
      '[GET https://api.example.com/hidden-basic-auth/.*]':
        credentials: PREEMPTIVE(admin:pwd)
```

## Interceptors Configuration

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        interceptors:
          '[myinter]': false
          '[tu-interceptor]': true
```

## Custom Request Context

```yaml
spring:
  httpclient5:
    request-config:
      '[GET https://api.example.com/.*]':
        custom-request-context:
          '[propA]': "value1"
          '[propB]': "value2"
```

## Base application properties (user-agent, JMX domain)

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.user-agent` | `HttpClient5/SpringBoot` | User-Agent header sent with all requests |
| `spring.httpclient5.jmx.domain` | `${spring.application.name}` | JMX monitoring domain for metrics collection |

## HTTP/1.0 specific settings

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `spring.httpclient5.http1.buffer-size` | `8192` | Size of internal buffer for HTTP communication |
| `spring.httpclient5.http1.waitForContinueTimeout` | `PT30S` | Timeout for waiting for CONTINUE response in HTTP/1.1 |

## Example Configuration File

For a complete working example, see `src/test/resources/application-example.yml`.