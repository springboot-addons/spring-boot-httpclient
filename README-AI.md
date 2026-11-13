# spring-boot-httpclient5

A Spring Boot starter for Apache HttpClient 5 with comprehensive configuration options.

## Features

- Support for both synchronous and asynchronous HTTP clients
- Comprehensive connection pooling configuration
- HTTP/1.0 specific configuration options
- Request-level configuration based on method and URI patterns
- JMX monitoring support
- Customizable request interceptors and context
- Authentication support (basic, preemptive)

## Configuration Options

### Basic Configuration

```yaml
spring:
  httpclient5:
    user-agent: HttpClient5/SRU
    jmx.domain: ${spring.application.name}
```

### HTTP/1.0 Specific Configuration

```yaml
spring:
  httpclient5:
    http1:
      buffer-size: 4096
      waitForContinueTimeout: PT32S
```

### Connection Pool Configuration

#### Synchronous Pool
```yaml
spring:
  httpclient5:
    pool:
      max-conn-total: 128
      max-connper-route: 30
      dns-resolver: system
      conn-pool-policy: LIFO
      connection-time-to-live: PT120S
      pool-concurrency-policy: STRICT
      default-connection-config:
        connect-timeout: PT0.5S
        socket-timeout: PT30S
      default-socket-config:
        so-timeout: PT60S
      host-config:
        '[http://example.com]':
          connect-timeout: PT1S
          max-connections: 10
```

#### Asynchronous Pool
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

### Request Configuration

Request configurations are applied based on method and URI patterns:

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
      
      # Specific configuration for GET requests to httpbin host
      '[GET ${app.httpbin-host}/.*]':
        response-timeout: 30000
        interceptors:
          '[myinter]': true
          '[tu-interceptor]': true
        custom-request-context:
          '[propA]': "surcharge"
          '[propB]': "newOne"
      
      # Specific configuration for requests with basic auth
      '[GET ${app.httpbin-host}/basic-auth/.*]':
        credentials: admin:pwd
      
      # Specific configuration for requests with proxy
      '[.* https://testhost:4443/.*]':
        proxy: https://localhost:3128
```

## Usage

Add the dependency to your project:

```xml
<dependency>
    <groupId>io.github.springboot</groupId>
    <artifactId>spring-boot-starter-httpclient5</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then inject the HttpClient in your service:

```java
@Service
public class MyService {
    
    private final HttpClient httpClient;
    
    public MyService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    // Use httpClient for making requests
}
```

## Testing

The configuration has been tested with various scenarios including:
- Default configuration values
- Host-specific configurations
- Method and URI pattern matching
- Request interceptor support
- Authentication configurations

For a complete example of configuration, see `src/test/resources/application-example.yml`.