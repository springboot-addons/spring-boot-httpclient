# spring-boot-httpclient

Spring Boot auto-configuration for **Apache HttpClient 5**: synchronous and asynchronous clients,
connection pools, per-request configuration, authentication, retries, header propagation, Actuator
metrics and Resilience4j integration.

Published on Maven Central under `io.github.springboot-addons`.

## Compatibility

| Spring Boot | HttpClient 5 | Starter line | Recommended version |
|---|---|---|---|
| 3.3.x | 5.3.x | 1.0.x | `1.0.6` |
| 3.4.x | 5.4.x | 1.2.x | `1.2.1` |
| 3.5.x | 5.5.x | 1.3.x | `1.3.1` |
| 4.0.x | 5.5.x | 2.0.x | `2.0.2` |
| 4.1.x | 5.6.x | 2.1.x | `2.1.2` |
| 4.2.x | 5.6.x | 2.2.x | `2.2.1` |

"Recommended" is the latest release of the corresponding starter line.

`hc5-sb4` is the maintenance branch for Spring Boot 4.0.x; current version `2.0.3-SNAPSHOT`,
last release `2.0.2`. Fixes are backported from `develop` by cherry-pick.

Java 17 is the runtime target. The build runs on JDK 17 up to JDK 25.

## Modules

| Artifact | Description |
|---|---|
| `spring-boot-starter-httpclient5` | Core: sync/async clients, pools, request rules, retries, authentication |
| `spring-boot-starter-httpclient5-web` | Bidirectional HTTP header propagation, `@Async` context propagation, `RestTemplate` customization |
| `spring-boot-starter-httpclient5-actuator` | Micrometer observations and connection-pool metrics |
| `spring-boot-starter-httpclient5-resilience4j` | Resilience4j circuit breaker and rate limiter integration |
| `spring-boot-starter-httpclient5-all` | POM aggregator for the four starters above |
| `spring-boot-starter-httpclient5-integration-tests` | End-to-end sample and test application |

## Quick start

```xml
<dependency>
    <groupId>io.github.springboot-addons</groupId>
    <artifactId>spring-boot-starter-httpclient5</artifactId>
    <version>2.0.2</version>
</dependency>
```

The starter registers:

- `CloseableHttpClient` (injectable as `HttpClient`)
- `CloseableHttpAsyncClient` (started)
- `PoolingHttpClientConnectionManager` and `PoolingAsyncClientConnectionManager`
- `Executor` (Apache HttpClient fluent API)

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
}
```

Add `spring-boot-starter-httpclient5-all` (type `pom`) to get the web, actuator and Resilience4j
starters at once.

## Behaviour with no configuration

The starter is usable without any property. With no configuration it applies:

- a default request rule: connection-request timeout **3 s**, response timeout **30 s**;
- for both pools: max connections per route **50**, concurrency policy **LAX**,
  connect timeout **3 s**, socket timeout **30 s**, connection TTL **120 s**,
  validate-after-inactivity **20 s**; the synchronous pool also gets SO_TIMEOUT **30 s**;
  idle connections are closed after **30 s**;
- total connections are not limited (`max-conn-total` unset).

Set `spring.httpclient5.autoconfig=false` to keep the Apache defaults instead.

Recommended minimal configuration:

```yaml
spring:
  httpclient5:
    user-agent: my-application/1.0
    pool:
      max-conn-total: 128
      max-connper-route: 30
      default-connection-config:
        connect-timeout: PT1S
        socket-timeout: PT30S
    async-pool:
      max-connper-route: 20
    request-config:
      '[default]':
        connection-request-timeout: PT1S
        response-timeout: PT10S
```

> `max-connper-route` is the historical key. The standard relaxed form `max-conn-per-route` also
> binds.

## Request-level configuration

Rules are matched against `method + " " + absolute URI` with Java regular expressions
(full match), and merged in lexicographic key order on top of `[default]`:

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        response-timeout: PT10S
        retry-config:
          max-retries: 2
          retry-interval: PT1S
        interceptors:
          '[my-interceptor]': true
        custom-request-context:
          '[tenant]': default

      '[GET https://api.example.com/.*]':
        response-timeout: PT30S

      '[GET https://api.example.com/basic-auth/.*]':
        credentials: user:password

      '[GET https://api.example.com/hidden-basic-auth/.*]':
        credentials: PREEMPTIVE(user:password)

      '[POST https://api.example.com/.*]':
        proxy: http://localhost:3128
        error-management:
          circuit-name: api
          max-attempts: 3
          wait-duration: 200
          broken-circuit-action: 503
```

What is available per rule:

- all standard Apache `RequestConfig` builder properties (`response-timeout`,
  `connection-request-timeout`, `connection-keep-alive`, `max-redirects`, `proxy`, ...);
- `credentials`: `user:password`, `BASIC(user:password)` or `PREEMPTIVE(user:password)`;
- `retry-config`: transport-level retries (`max-retries`, `retry-interval`);
- `interceptors`: enable/disable any `ActivableHttpInterceptor` bean by its id;
- `custom-request-context`: attributes stored in the `HttpContext` for the exchange;
- `error-management`: Resilience4j circuit breaker/rate limiter settings;
- `headers-propagation`: web starter header rules (see below).

## Header propagation (web starter)

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        headers-propagation:
          enabled: true
          down:            # incoming request headers forwarded to outgoing calls
            - X-Request-.*
          up:              # outgoing response headers copied to the incoming response
            - X-Trace-.*
          add:             # static headers added to outgoing calls
            '[X-Application]': my-app
          remove:          # headers removed from outgoing calls
            - X-Internal-.*
```

The web starter also:

- propagates the request context to `@Async` tasks (including virtual threads);
- customizes Boot's `RestTemplateBuilder`/`RestTemplate` to use the starter's `HttpClient`.

## Actuator and Resilience4j

Add `spring-boot-starter-httpclient5-actuator` for Micrometer observations and pool metrics:

| Meter | Type | Tags |
|---|---|---|
| `http.client.request.duration` | timer | `http.request.method`, `server.address`, `server.port`, `http.response.status_code`, `error.type`, `outcome` |
| `httpcomponents.httpclient.pool.total.connections` | gauge | `httpclient`, `state=available\|leased` |
| `httpcomponents.httpclient.pool.total.pending` | gauge | `httpclient` |
| `httpcomponents.httpclient.pool.total.max` | gauge | `httpclient` |
| `httpcomponents.httpclient.pool.route.max.default` | gauge | `httpclient` |

See [Actuator metrics](docs/actuator-metrics.md) for PromQL queries and version differences.
When the adaptive response timeout is enabled, the actuator module also exposes
`httpclient5.adaptive.timeout`, `.percentile`, `.samples` and `.timeouts`.

### Adaptive response timeout (actuator starter)

The actuator module can replace the static `response-timeout` with a value derived from the
observed time-to-first-byte (TTFB). The policy is global and can be enabled or disabled per
request rule:

```yaml
spring:
  httpclient5:
    adaptive-timeout:
      enabled: true
      percentile: 0.99
      factor: 1.5
      lower-bound-factor: 2.0
      warmup-requests: 100
      window: PT5M
      max-timeout: PT60S
    request-config:
      '[GET https://api.example.com/stream/.*]':
        adaptive-timeout:
          enabled: false
```

Leave `spring.httpclient5.adaptive-timeout.enabled` unset to keep the policy disabled by default,
then set `adaptive-timeout.enabled: true` on the rules that should use it.

The timeout is `clamp(p99 × 1.5, max(2 × min observed TTFB, min-timeout), max-timeout)`. It is
applied only after the warmup and the minimum number of samples.

The Actuator endpoint `/actuator/httpclient5-adaptive-timeout` exposes the computed timeout and the
per-rule delta with the configured timeout. A DEBUG log (every minute by default) reports only the
rules whose effective timeout differs. See the
[configuration guide](docs/configuration-guide.md#adaptive-response-timeout) and the
[actuator metrics](docs/actuator-metrics.md#adaptive-timeout).

Add `spring-boot-starter-httpclient5-resilience4j` to apply a Resilience4j circuit breaker and,
when the circuit name is not `default`, a rate limiter with the same name:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      api:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  ratelimiter:
    instances:
      api:
        limitForPeriod: 20
        limitRefreshPeriod: 1s
```

```yaml
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

HTTP 5xx responses and exceptions count as failures. `broken-circuit-action` accepts `error`
(default, throws an exception) or an HTTP status code returned when the circuit is open.

## Documentation

- [Configuration guide](docs/configuration-guide.md) – concepts, pools, request rules, web/actuator/Resilience4j
- [Configuration reference](docs/CONFIGURATION-ALL.md) – all properties and defaults
- [Actuator metrics](docs/actuator-metrics.md) – meters, tags, Prometheus names and queries
- [Usage examples](docs/usage-example.md) – sync, async, fluent, `RestTemplate`, header propagation
- [AGENTS.md](AGENTS.md) – branch workflow, build, test and release procedure for contributors

## Build and test

```bash
./mvnw clean verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

The tests use Testcontainers with a local [`go-httpbin`](https://github.com/mccutchen/go-httpbin)
container: **Docker must be running**. No external test endpoint is used.

## Release

A release is a single command that prepares, signs, deploys and publishes to Maven Central:

```powershell
.\mvnw.cmd release:prepare release:perform
```

One-time setup: GPG key, `MAVEN_GPG_PASSPHRASE` environment variable, Maven Central credentials
(server id `central`), branch tracking `origin`, Docker running. See [AGENTS.md](AGENTS.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).
