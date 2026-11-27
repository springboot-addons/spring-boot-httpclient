# Configuration guide

This guide describes how `spring-boot-httpclient5` builds and configures Apache HttpClient 5.
All properties use the `spring.httpclient5` prefix and Spring Boot relaxed binding.

## Quick example

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
    request-config:
      '[default]':
        connection-request-timeout: PT1S
        response-timeout: PT10S
      '[GET https://api.example.com/.*]':
        response-timeout: PT30S
        credentials: user:password
```

## Contents

1. [How configuration is applied](#how-configuration-is-applied)
2. [Connection pools](#connection-pools)
3. [Request rules](#request-rules)
4. [Authentication](#authentication)
5. [Retries](#retries)
6. [Interceptors and request context](#interceptors-and-request-context)
7. [Resilience4j](#resilience4j)
8. [Web starter](#web-starter)
9. [Actuator](#actuator)
10. [Extension points](#extension-points)
11. [Zero-configuration defaults](#zero-configuration-defaults)

---

## How configuration is applied

The starter binds `spring.httpclient5.*` to `HttpClient5Config` (`@ConfigurationProperties`).

At startup:

1. `spring.httpclient5.pool` builds the synchronous `PoolingHttpClientConnectionManager`.
2. `spring.httpclient5.async-pool` builds the asynchronous `PoolingAsyncClientConnectionManager`.
3. `spring.httpclient5.request-config` rules are indexed; placeholders are expanded and the
   `[default]` entry is used as the base rule.
4. A `CloseableHttpClient`, a started `CloseableHttpAsyncClient` and a fluent `Executor` are created.
5. Request interceptors resolve the matching rule and apply it to an `HttpContext`.

All standard Apache HttpClient builder properties are accepted (connection config, socket config,
HTTP/1 config, I/O reactor config, `RequestConfig`). See the Apache HttpClient 5 Javadoc for the
complete list: <https://hc.apache.org/httpcomponents-client-5.6.x/current/httpclient5/apidocs/>.

## Connection pools

`pool` is synchronous, `async-pool` is asynchronous. Both use Apache connection-manager builders;
the asynchronous builder has no `default-socket-config` (socket settings are carried by
`default-connection-config.socket-timeout`).

```yaml
spring:
  httpclient5:
    pool:
      max-conn-total: 128
      max-connper-route: 30
      connection-idle-timeout: PT30S
      default-connection-config:
        connect-timeout: PT1S
        socket-timeout: PT30S
        time-to-live: PT120S
        validate-after-inactivity: PT20S
      default-socket-config:
        so-timeout: PT60S
        so-keepalive: true
        tcp-no-delay: true
      host-config:
        '[https://api.example.com]':
          connect-timeout: PT2S
          socket-timeout: PT45S
          max-connections: 20
    async-pool:
      max-connper-route: 20
      pool-concurrency-policy: LAX
```

### Pool-level properties

| Property | Description |
|---|---|
| `max-conn-total` | Maximum total connections; unset means unlimited |
| `max-connper-route` (or `max-conn-per-route`) | Maximum connections per route |
| `dns-resolver` | `system`, `in_memory`, or a `DnsResolver` implementation class name |
| `conn-pool-policy` | Connection reuse policy (`LIFO` / `FIFO`) |
| `connection-time-to-live` | Pool-level connection TTL |
| `pool-concurrency-policy` | `STRICT` or `LAX` |
| `connection-idle-timeout` | Idle connections older than this are closed periodically (default 30 s) |
| `default-connection-config` | `ConnectionConfig` applied to routes without a specific host config |
| `default-socket-config` | `SocketConfig` applied by default (synchronous pool only) |
| `host-config` | Per-host `ConnectionConfig`, plus `max-connections` |

### Host configuration

Keys are host URIs (`https://api.example.com`). Placeholders are expanded and the default port is
added (`http` → `:80`, `https` → `:443`) before matching. A host entry can override:

- the standard `ConnectionConfig` properties (`connect-timeout`, `socket-timeout`, `time-to-live`,
  `validate-after-inactivity`, `idle-timeout`);
- `max-connections`, which sets the max-per-route value for that host.

If a proxy is configured for that host in `request-config` (rule `GET <host>/`), the route planner
uses the proxied route and the host max-per-route applies to it.

### Inactivity cleanup

A single scheduler closes expired connections and idle connections older than
`connection-idle-timeout`. It starts every `connection-idle-timeout / 10` (3 s with the default
30 s).

## Request rules

`spring.httpclient5.request-config` is a map of **regular expressions** matched against
`METHOD + " " + absolute URI`:

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        response-timeout: PT10S
      '[GET https://api.example.com/.*]':
        response-timeout: PT30S
      '[POST https://api.example.com/.*]':
        connection-request-timeout: PT2S
```

Rules:

- The regex must match the whole string (`String.matches`), so use `.*` where needed.
- `[default]` is the base rule. It is always applied first.
- All matching rules are merged in **lexicographic key order**; the last matching value wins.
  Write one distinct rule per behaviour, or rely on the alphabetical order knowingly.
- `${...}` placeholders are resolved at startup, including inside keys.
- The resolved configuration is cached per `(method, uri)`.

Every standard Apache `RequestConfig` builder property is available, for example:
`response-timeout`, `connection-request-timeout`, `connection-keep-alive`, `redirects-enabled`,
`max-redirects`, `authentication-enabled`, `content-compression-enabled`, `proxy`, `cookie-spec`.

### Starter-specific request properties

```yaml
spring:
  httpclient5:
    request-config:
      '[GET https://api.example.com/.*]':
        credentials: user:password
        retry-config:
          max-retries: 2
          retry-interval: PT1S
        interceptors:
          '[my-interceptor]': true
        custom-request-context:
          '[tenant]': acme
        error-management:
          circuit-name: api
```

## Adaptive response timeout

The adaptive response timeout is an optional, **global** policy implemented in
`spring-boot-starter-httpclient5-actuator`. When enabled, the starter replaces the
`response-timeout` of a request with a value derived from the observed time-to-first-byte (TTFB)
of recent requests:

1. the configured percentile (default `0.99`) of the samples in the sliding window;
2. multiplied by a safety factor (default `1.5`);
3. floored by `max(min-timeout, lower-bound-factor × min observed TTFB)` (default factor `2.0`);
4. capped by `max-timeout` (default `60s`).

The policy stays inactive until `warmup-requests` samples have been recorded and at least
`min-samples` samples are present in the window. While it is inactive, the static
`response-timeout` applies.

```yaml
spring:
  httpclient5:
    adaptive-timeout:
      enabled: true
      percentile: 0.99
      factor: 1.5
      lower-bound-factor: 2.0
      window: PT5M
      max-samples: 1000
      min-samples: 20
      warmup-requests: 100
      refresh-interval: PT10S
      min-timeout: PT0S
      max-timeout: PT60S
      include-timeouts: true
```

The policy is global: all sampled requests feed the same bounded window, including requests whose
rule disables the policy. Disabling a rule only prevents the computed value from being applied to
that rule. The global `enabled` flag is `false` by default. A rule can enable the policy even when
the global flag is false, or disable it when the global flag is true:

```yaml
spring:
  httpclient5:
    # Global switch: false by default. Leave it unset to disable the policy by default.
    # adaptive-timeout:
    #   enabled: false
    request-config:
      # Enable the policy only for this rule
      '[GET https://api.example.com/.*]':
        adaptive-timeout:
          enabled: true
      # Explicitly disable it for a long-lived stream
      '[GET https://api.example.com/stream/.*]':
        adaptive-timeout:
          enabled: false
```

The `enabled` flag of a rule overrides the global flag for the matching requests; when it is
absent, the global value applies. The policy parameters (`percentile`, `factor`, `window`, ...)
remain global and apply to every rule that enables the policy.

| Property | Default | Description |
|---|---|---|
| `enabled` | `false` | Global switch; a rule can override it |
| `percentile` | `0.99` | TTFB percentile used for the computation |
| `factor` | `1.5` | Safety multiplier applied to the percentile |
| `lower-bound-factor` | `2.0` | Lower bound = this factor × the minimum observed TTFB |
| `window` | `PT5M` | Sliding window used to select samples |
| `max-samples` | `1000` | Maximum samples kept in memory |
| `min-samples` | `20` | Minimum samples required in the window |
| `warmup-requests` | `100` | Samples recorded before the policy starts |
| `refresh-interval` | `PT10S` | Minimum delay between two recalculations |
| `min-timeout` | `PT0S` | Optional absolute lower bound |
| `max-timeout` | `PT60S` | Upper bound |
| `include-timeouts` | `true` | Record response timeouts as censored samples |
| `diagnostics.log-enabled` | `true` | Register the scheduled DEBUG log of differences |
| `diagnostics.log-interval` | `PT1M` | Period between two diagnostic logs |
| `diagnostics.log-max-rules` | `20` | Maximum differing rules logged per period |

Limits:

- `response-timeout` is a socket inactivity timeout, not a total deadline. The policy uses the
  TTFB as a proxy; a slow body or a streaming response is not reflected in the sample.
- The policy is evaluated per JVM and is not shared between instances.
- It has no effect on HTTP/2 multiplexed connections, where HttpClient ignores
  `response-timeout`.
- The measured duration includes retries and redirects handled below the sampling handler.
- A timed-out request is recorded at the effective timeout when `include-timeouts` is `true`.
  This is what prevents the percentile from collapsing; set it to `false` to compute the
  percentile on successful responses only.
- A timeout is seen as a failure by the Resilience4j circuit breaker.
- Disable the policy per rule for long-lived streams or endpoints whose latency is not
  representative.

### Diagnostics

The actuator endpoint `/actuator/httpclient5-adaptive-timeout` exposes the current computed
timeout, the policy state and, for each rule, the configured/inherited timeout, the effective
timeout and the delta. Expose it with:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,httpclient5-adaptive-timeout
```

The endpoint reports a `configuredSource` of `rule`, `default` or `unset` and a `differs` flag per
rule. See the [Actuator metrics guide](actuator-metrics.md#adaptive-timeout-diagnostics-endpoint)
for the full response.

A DEBUG log is emitted every `diagnostics.log-interval` (one minute by default) for the rules whose
effective timeout differs from the configured one. Only differences are logged:

```yaml
logging:
  level:
    io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutDiagnosticsLogger: DEBUG
```

Set `diagnostics.log-enabled=false` to remove the scheduled task, and `diagnostics.log-max-rules` to
cap the number of lines per period. The endpoint remains available when the log is disabled.

## Authentication

The `credentials` value accepts three forms:

```yaml
credentials: user:password
credentials: BASIC(user:password)
credentials: PREEMPTIVE(user:password)
```

- `user:password` and `BASIC(...)` use standard (challenge-based) basic authentication.
- `PREEMPTIVE(...)` sends the `Authorization` header immediately; any existing `Authorization`
  header on the request is kept.
- The first `:` separates the user from the password; the password may contain `:` characters.
- Malformed values (no colon, blank user, unclosed prefix) are rejected.

## Retries

Two independent retry mechanisms exist:

1. **Transport retries** – `retry-config.max-retries` / `retry-interval` in a request rule. They are
   handled by the client retry strategy. Idempotent methods are retried on I/O errors; `429` and
   `503` responses are retried. The default interval is 1 second when not configured.
2. **Resilience4j retries** – `error-management.max-attempts` / `wait-duration`, only with the
   Resilience4j starter (below).

The retry configuration from the matching request rule is used for each exchange.

## Interceptors and request context

A bean extending `ActivableHttpInterceptor` has an id:

```java
@Component
public class TraceInterceptor extends ActivableHttpInterceptor {
    public TraceInterceptor() {
        super("trace");
    }
    // ...
}
```

It is only invoked when the matching request rule enables it:

```yaml
interceptors:
  '[trace]': true
```

`custom-request-context` entries are stored as `HttpContext` attributes for the exchange and are
removed after the response.

## Resilience4j

Add `spring-boot-starter-httpclient5-resilience4j`. The integration is enabled by default
(`spring.httpclient5.core.resilience4j.enabled=true`).

Define normal Resilience4j instances:

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
```

Reference the circuit in the request rule:

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

Semantics:

- HTTP 5xx responses and thrown exceptions are recorded as failures; other responses are successes.
- `max-attempts` is passed to Resilience4j `Retry` (1 = no retry). `wait-duration` is in milliseconds.
- When the circuit is open, `broken-circuit-action` controls the result:
  - `error` (default): throws a `ClientProtocolException` containing `Broken circuit`;
  - a numeric value (e.g. `503`): returns that HTTP status with reason `Broken circuit`.
- When `circuit-name` is not `default`, a **rate limiter with the same name** is also used. Define
  `resilience4j.ratelimiter.instances.<circuit-name>` if rate limiting is required, and leave
  `circuit-name` at `default` to disable rate limiting.

## Web starter

Add `spring-boot-starter-httpclient5-web` for header propagation, async context propagation and
`RestTemplate` integration.

### Header propagation

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

| Key | Direction | Meaning |
|---|---|---|
| `down` | incoming → outgoing | Regex list of request header names collected from the servlet request and added to outgoing HttpClient requests |
| `up` | outgoing → incoming | Regex list of response header names copied from HttpClient responses to the servlet response |
| `add` | outgoing | Static headers added to outgoing requests |
| `remove` | outgoing | Header names removed from outgoing requests |

`headers-propagation.enabled` is per rule and defaults to unset (disabled). The web auto-configuration
itself is enabled by default.

### Async and virtual threads

`AsyncExecutorConfig` registers a `TaskDecorator` that copies the request context to `@Async` tasks.
This keeps request-scoped header storages available with both platform and virtual threads.
Disable it with `spring.httpclient5.web.async-decorator.enabled=false`.

### RestTemplate

The starter registers a primary `RestTemplateCustomizer` that configures Boot's `RestTemplate` with
the starter's `HttpClient`:

```java
@Service
public class MyService {
    private final RestTemplate restTemplate;

    public MyService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }
}
```

Disable it with `spring.httpclient5.web.rest-template.enabled=false`.

## Actuator

Add `spring-boot-starter-httpclient5-actuator` and `spring-boot-starter-actuator`. The module
registers:

- an `ObservationExecChainHandler` that records `http.client.request.duration` for the sync and
  async clients;
- connection-pool gauges for both managers, tagged `httpclient=httpclient5.pool` and
  `httpclient=httpclient5.async-pool`.

The main meters are:

| Meter | Type | Description |
|---|---|---|
| `http.client.request.duration` | timer | Request latency, tagged by method, host, port, status, outcome and error type |
| `httpcomponents.httpclient.pool.total.connections` | gauge | `available` and `leased` connections |
| `httpcomponents.httpclient.pool.total.pending` | gauge | Requests waiting for a connection |
| `httpcomponents.httpclient.pool.total.max` | gauge | Configured total connection limit |
| `httpcomponents.httpclient.pool.route.max.default` | gauge | Configured per-route limit |

Disable it with `spring.httpclient5.core.actuator.enabled=false`. See
[Actuator metrics](actuator-metrics.md) for tags, Prometheus names, queries and the metric-name
differences between starter lines.

## Extension points

- `RequestConfigCustomizer` bean: adjust the resolved `RequestConfigProperties` per method/URI.
- Custom `HttpClientBuilder` / `HttpAsyncClientBuilder` beans: the starter starts from them.
- Custom `CookieStore`, `CredentialsStore`, `HttpRequestInterceptor`, `HttpResponseInterceptor`,
  `ExecChainHandler`, `AsyncExecChainHandler`, `ConnectionReuseStrategy`,
  `ConnectionKeepAliveStrategy`, `HttpRequestRetryStrategy` beans are picked up automatically.
- `spring.httpclient5.core.enabled=false` disables the whole core auto-configuration.

## Zero-configuration defaults

With no pool/request configuration, the starter applies:

| Setting | Value |
|---|---|
| Request `connection-request-timeout` | 3 s |
| Request `response-timeout` | 30 s |
| Max connections per route (both pools) | 50 |
| Pool concurrency policy | `LAX` |
| Connect timeout | 3 s |
| Socket timeout | 30 s |
| Connection TTL | 120 s |
| Validate after inactivity | 20 s |
| SO_TIMEOUT (synchronous pool) | 30 s |
| Idle connection timeout | 30 s |
| Max total connections | unlimited |
| User-Agent | none |

`spring.httpclient5.autoconfig=false` keeps the Apache defaults instead of these values.
