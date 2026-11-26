# Configuration reference

All properties are prefixed with `spring.httpclient5`. Time values use ISO-8601 durations
(`PT30S`, `PT1M`, `PT0.5S`) unless stated otherwise. Standard Apache HttpClient builder properties
are accepted through relaxed binding.

## Base properties

| Property | Default | Description |
|---|---|---|
| `spring.httpclient5.user-agent` | unset | `User-Agent` sent by both clients |
| `spring.httpclient5.autoconfig` | `true` | Apply the zero-configuration defaults described below |
| `spring.httpclient5.core.enabled` | `true` | Enable/disable the core auto-configuration |
| `spring.httpclient5.jmx.domain` | `default` | Legacy field, not wired to JMX in 2.x (use Actuator/Micrometer) |
| `spring.httpclient5.jmx.metric-name-strategy` | `HOST_AND_METHOD` | Legacy field, not used in 2.x |

## HTTP/1 configuration

`spring.httpclient5.http1.*` maps to Apache `Http1Config.Builder`. Apache defaults:

| Property | Default | Description |
|---|---|---|
| `http1.buffer-size` | `32768` | Internal buffer size |
| `http1.chunk-size-hint` | `-1` | Chunk size hint |
| `http1.wait-for-continue-timeout` | unset | Timeout for `Expect: 100-continue` |
| `http1.max-line-length` | `8192` | Maximum line length |
| `http1.max-header-count` | `100` | Maximum number of headers |
| `http1.max-empty-line-count` | `10` | Maximum tolerated empty lines |

## Character coding and I/O reactor

`spring.httpclient5.char-coding.*` maps to `CharCodingConfig.Builder`
(`charset`, `malformed-input-action`, `unmappable-input-action`).

`spring.httpclient5.io-reactor.*` maps to `IOReactorConfig.Builder`
(`select-interval`, `io-thread-count`, `so-timeout`, `so-reuse-address`, `so-linger`,
`so-keepalive`, `tcp-no-delay`, `snd-buf-size`, `rcv-buf-size`, `backlog-size`, ...).

## Connection pools

`pool` is the synchronous pool; `async-pool` is the asynchronous pool. Both expose the same
properties.

| Property | Zero-conf default | Description |
|---|---|---|
| `max-conn-total` | `0` (unlimited) | Maximum total connections |
| `max-connper-route` | `50` | Maximum connections per route (also binds as `max-conn-per-route`); the 50/LAX zero-conf values are skipped if either pool declares this property |
| `dns-resolver` | `system` | `system`, `in_memory`, or a `DnsResolver` class name |
| `conn-pool-policy` | `LIFO` (Apache default) | Connection reuse policy (`LIFO`/`FIFO`) |
| `connection-time-to-live` | unset | Pool-level connection TTL |
| `pool-concurrency-policy` | `LAX` | `STRICT` or `LAX` |
| `connection-idle-timeout` | `PT30S` | Idle connection timeout used by the cleaner |
| `default-connection-config` | see below | Default `ConnectionConfig` |
| `default-socket-config` | see below | Default `SocketConfig` (synchronous pool only) |
| `host-config` | `{}` | Per-host overrides |

### `default-connection-config`

Maps to `ConnectionConfig.Builder`, plus `max-connections` for host entries:

| Property | Zero-conf default | Description |
|---|---|---|
| `connect-timeout` | `PT3S` | TCP connect timeout |
| `socket-timeout` | `PT30S` | Socket read timeout |
| `time-to-live` | `PT120S` | Connection time-to-live |
| `validate-after-inactivity` | `PT20S` | Re-validate idle connections after this delay |
| `idle-timeout` | unset | Idle timeout |
| `max-connections` | unset | Host config only: max-per-route for that host |

> Prefer `default-connection-config.connect-timeout` / `host-config`. The
> `RequestConfig.connect-timeout` property is deprecated in Apache HttpClient 5.2+.

### `default-socket-config`

Synchronous pool only (the asynchronous builder has no socket-config hook; use
`default-connection-config.socket-timeout` there). Maps to `SocketConfig.Builder`:

| Property | Zero-conf default | Description |
|---|---|---|
| `so-timeout` | `PT30S` | `SO_TIMEOUT` (synchronous pool) |
| `so-keepalive` | Apache default (`true`) | `SO_KEEPALIVE` |
| `tcp-no-delay` | Apache default (`true`) | `TCP_NODELAY` |
| `so-reuse-address`, `so-linger`, `snd-buf-size`, `rcv-buf-size`, `backlog-size`, `tcp-keep-idle`, `tcp-keep-interval`, `tcp-keep-count` | Apache defaults | Standard socket options |

### `host-config`

Keys are host URIs. Placeholders are expanded and default ports are normalized (`http` → `:80`,
`https` → `:443`). Values are `ConnectionConfig` properties plus `max-connections`:

```yaml
host-config:
  '[https://api.example.com]':
    connect-timeout: PT2S
    socket-timeout: PT45S
    time-to-live: PT300S
    max-connections: 20
```

## Request configuration

`spring.httpclient5.request-config` is a map of regex rules matched against `METHOD + " " + URI`.
`[default]` is the base rule. All matching rules are merged in lexicographic key order (last value
wins). Placeholders are resolved in keys and values.

### Standard `RequestConfig` properties

All `RequestConfig.Builder` properties are available. With no configuration, Apache
`RequestConfig.DEFAULT` applies; the starter's zero-conf rule overrides the two timeouts below.

| Property | Apache default | Starter zero-conf |
|---|---|---|
| `connection-request-timeout` | 3 min | **3 s** |
| `response-timeout` | unset | **30 s** |
| `connection-keep-alive` | 3 min | 3 min |
| `redirects-enabled` | `true` | `true` |
| `max-redirects` | `50` | `50` |
| `authentication-enabled` | `true` | `true` |
| `content-compression-enabled` | `true` | `true` |
| `protocol-upgrade-enabled` | `true` | `true` |
| `hard-cancellation-enabled` | `true` | `true` |
| `proxy` | unset | unset |
| `cookie-spec` | unset | unset |
| `connect-timeout` | unset | unset (deprecated, use connection config) |

### Starter-specific request properties

| Property | Default | Description |
|---|---|---|
| `credentials` | unset | `user:password`, `BASIC(user:password)` or `PREEMPTIVE(user:password)` |
| `retry-config.max-retries` | `0` | Transport-level retry count |
| `retry-config.retry-interval` | `PT1S` | Interval between transport retries |
| `interceptors` | `{}` | `ActivableHttpInterceptor` id → enabled flag |
| `custom-request-context` | `{}` | Attributes added to the `HttpContext` for the exchange |
| `error-management.circuit-name` | `default` | Resilience4j circuit breaker name |
| `error-management.max-attempts` | `1` | Resilience4j retry attempts (1 = no retry) |
| `error-management.wait-duration` | `0` | Resilience4j retry wait, in milliseconds |
| `error-management.broken-circuit-action` | `error` | `error` or an HTTP status code |
| `headers-propagation.enabled` | unset (disabled) | Enable header propagation for this rule |
| `headers-propagation.down` | `[]` | Regex list: incoming request headers forwarded to outgoing calls |
| `headers-propagation.up` | `[]` | Regex list: outgoing response headers copied to the incoming response |
| `headers-propagation.add` | `{}` | Static headers added to outgoing calls |
| `headers-propagation.remove` | `[]` | Headers removed from outgoing calls |

## Auto-configuration switches

| Property | Default | Module |
|---|---|---|
| `spring.httpclient5.core.enabled` | `true` | core |
| `spring.httpclient5.core.actuator.enabled` | `true` | actuator |
| `spring.httpclient5.core.resilience4j.enabled` | `true` | resilience4j |
| `spring.httpclient5.web.headers-filter.enabled` | `true` | web |
| `spring.httpclient5.web.headers-propagation.enabled` | `true` | web |
| `spring.httpclient5.web.async-decorator.enabled` | `true` | web |
| `spring.httpclient5.web.rest-template.enabled` | `true` | web |

## Resilience4j instances

Instances are standard Spring Boot Resilience4j properties:

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

- `circuit-name` selects `resilience4j.circuitbreaker.instances.<name>`.
- For any name other than `default`, a `RateLimiter` with the same name is also used; define it in
  `resilience4j.ratelimiter.instances.<name>` if you need rate limiting.
- HTTP 5xx and exceptions are failures. `broken-circuit-action` handles the open-circuit response.

## Actuator metrics

The actuator module adds:

- request observations (`http.client.request.duration` and related metrics);
- pool metrics bound under `httpclient5.pool` and `httpclient5.async-pool`.

No additional configuration is required beyond the Actuator starter.
