# Usage examples

## Synchronous client

Inject the auto-configured `CloseableHttpClient` (or the `HttpClient` interface):

```java
@Service
public class ApiService {

    private final CloseableHttpClient httpClient;

    public ApiService(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    public int post(String url, String json) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            return response.getCode();
        }
    }
}
```

Timeouts, proxy, authentication and retries are applied automatically from the matching
`request-config` rule. No per-call code is required.

## Fluent API

The starter also exposes an Apache `Executor`:

```java
@Service
public class FluentApiService {

    private final Executor executor;

    public FluentApiService(Executor executor) {
        this.executor = executor;
    }

    public String get(String url) throws IOException {
        return executor.execute(Request.get(url))
                .returnContent()
                .asString();
    }
}
```

## Asynchronous client

`CloseableHttpAsyncClient` is created and started by the starter:

```java
@Service
public class AsyncApiService {

    private final CloseableHttpAsyncClient httpClient;

    public AsyncApiService(CloseableHttpAsyncClient httpClient) {
        this.httpClient = httpClient;
    }

    public String get(String url) throws Exception {
        SimpleHttpRequest request = SimpleHttpRequest.create("GET", url);
        Future<SimpleHttpResponse> future = httpClient.execute(request, null);
        SimpleHttpResponse response = future.get(5, TimeUnit.SECONDS);
        return response.getBodyText();
    }
}
```

The asynchronous pool, timeouts, retries and resilience settings are the same as for the
synchronous client.

## Header propagation (web starter)

Add `spring-boot-starter-httpclient5-web` and enable propagation on the rules you need:

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        headers-propagation:
          enabled: true
          down:
            - X-Request-.*
          up:
            - X-Trace-.*
          add:
            '[X-Application]': my-app
          remove:
            - X-Internal-.*
```

With this configuration:

- every incoming servlet request header matching `X-Request-.*` is stored for the current request;
- outgoing HttpClient calls in the same request automatically receive those headers;
- outgoing responses' `X-Trace-*` headers are copied to the incoming servlet response;
- `X-Application` is always added to outgoing calls and `X-Internal-*` headers are removed.

This also works from `@Async` methods (including virtual threads), because the web starter
propagates the request context to the async task.

## RestTemplate (web starter)

The starter customizes Boot's `RestTemplateBuilder` to use its `HttpClient`:

```java
@Service
public class RestApiService {

    private final RestTemplate restTemplate;

    public RestApiService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public String get(String url) {
        return restTemplate.getForObject(url, String.class);
    }
}
```

## Resilience4j

```yaml
resilience4j:
  circuitbreaker:
    instances:
      api:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  ratelimiter:
    instances:
      api:
        limitForPeriod: 20
        limitRefreshPeriod: 1s

spring:
  httpclient5:
    request-config:
      '[GET https://api.example.com/.*]':
        error-management:
          circuit-name: api
          max-attempts: 2
          wait-duration: 250
          broken-circuit-action: 503
```

- 5xx responses and exceptions open the circuit when the failure rate is exceeded;
- `max-attempts` and `wait-duration` drive the Resilience4j retry;
- when the circuit is open, `broken-circuit-action: 503` returns HTTP 503, while `error` throws;
- because the circuit name is `api` (not `default`), a `RateLimiter` named `api` is also used.

## Per-request customization in code

For behaviour that cannot be expressed with a pattern, register a `RequestConfigCustomizer` bean:

```java
@Configuration
public class HttpClientCustomization {

    @Bean
    public RequestConfigCustomizer tenantCustomizer() {
        return (method, uri, properties) -> {
            if (uri.contains("/tenant/")) {
                properties.setResponseTimeout(Timeout.ofSeconds(60));
            }
            return properties;
        };
    }
}
```

The customizer receives the merged `RequestConfigProperties`; changing the returned object is
enough.

To customize the Apache clients themselves, provide an `HttpClientBuilder`,
`HttpAsyncClientBuilder`, `CookieStore`, `CredentialsStore`, interceptor or exec-chain handler bean.
The starter picks these beans up when building the clients.

## Testing your configuration

The project's own tests are a good source of examples:

- `spring-boot-starter-httpclient5/src/test/resources/application.yml` – request rules, pools,
  authentication, interceptors;
- `spring-boot-starter-httpclient5-web/src/test/resources/application.yml` – header propagation;
- `spring-boot-starter-httpclient5-resilience4j/src/test/resources/application.yml` – circuit
  breaker and rate limiter;
- `spring-boot-starter-httpclient5-integration-tests` – end-to-end sample application.

The test suite uses Testcontainers with a local `go-httpbin` container (Docker required) and does
not depend on any external service.
