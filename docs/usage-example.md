# Usage Example for spring-boot-httpclient5

## Basic Service Usage

Here's how to use the configured HttpClient in your Spring Boot service:

```java
@Service
public class ApiService {
    
    private final HttpClient httpClient;
    
    public ApiService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public String getData(String url) throws IOException {
        // The client will automatically apply configuration based on URL and method
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(url))) {
            return EntityUtils.toString(response.getEntity());
        }
    }
    
    public String postData(String url, String data) throws IOException {
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(data));
        
        // Configuration will be applied automatically based on URL pattern
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
```

## Using with Spring WebFlux (Reactive)

```java
@Service
public class ReactiveApiService {
    
    private final AsyncHttpClient asyncHttpClient;
    
    public ReactiveApiService(AsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }
    
    public Mono<String> getData(String url) {
        return asyncHttpClient.execute(
            new HttpGet(url),
            new BasicResponseHandler()
        );
    }
}
```

## Configuration-Based Request Handling

The client automatically applies different configurations based on the request:

```java
// This request will use configuration for GET requests to httpbin host
// with a 30 second timeout and interceptors enabled
String result = apiService.getData("http://localhost:8080/get");

// This request will use default configuration with 5 second timeout
String result2 = apiService.getData("https://api.example.com/data");
```

## Using spring-boot-starter-httpclient5-web

With the web starter, you can configure header propagation:

```java
@Service
public class WebApiService {
    
    private final HttpClient httpClient;
    
    public WebApiService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public String getDataWithHeaders(String url) throws IOException {
        // Headers will be automatically propagated based on configuration
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(url))) {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
```

## Using spring-boot-starter-httpclient5-resilience4j

With the resilience4j starter, circuit breaker integration is automatic:

```java
@Service
public class ResilientApiService {
    
    private final HttpClient httpClient;
    
    public ResilientApiService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public String getDataWithCircuitBreaker(String url) throws IOException {
        // Circuit breaker will automatically be applied based on configuration
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(url))) {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
```

## Using spring-boot-starter-httpclient5-actuator

With the actuator starter, monitoring endpoints are available:

```java
@Service
public class MonitoredApiService {
    
    private final HttpClient httpClient;
    
    public MonitoredApiService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public String getDataWithMonitoring(String url) throws IOException {
        // Metrics will be automatically collected
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(url))) {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
```

## Custom Configuration in Code

If you need to override configurations programmatically:

```java
@Service
public class CustomConfigService {
    
    private final HttpClient httpClient;
    private final HttpClient5Config config;
    
    public CustomConfigService(HttpClient httpClient, HttpClient5Config config) {
        this.httpClient = httpClient;
        this.config = config;
    }
    
    public String executeWithCustomConfig(String url) throws IOException {
        // Create a new request with custom configuration
        HttpGet request = new HttpGet(url);
        
        // Apply specific configuration based on URL pattern
        RequestConfig customConfig = config.getRequestConfig("GET", url).build();
        request.setConfig(customConfig);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
```

## Testing the Configuration

The configuration has been tested with these scenarios:

1. Default configuration loading
2. Host-specific configurations
3. Method and URI pattern matching
4. Request interceptor support
5. Authentication configuration
6. Proxy configuration
7. Header propagation (web starter)
8. Circuit breaker integration (resilience4j starter)
9. Monitoring capabilities (actuator starter)

For complete examples, see the test files in `src/test/java/io/github/springboot/httpclient5/core/config/`.