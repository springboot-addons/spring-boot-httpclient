# Configuration reference

Reference for `spring-boot-httpclient5` 2.1.x. Every key is written **in full** so it can be copied
directly into `application.yml` / `application.properties`.

- Timeouts use ISO-8601 durations: `PT30S`, `PT1M`, `PT0.5S`.
- `<rule>` means either `[default]` or a request pattern such as
  `[GET https://api.example.com/.*]`.
- `<host>` means a host URI such as `https://api.example.com`.

This page is ordered by usefulness: request rules and pools first, low-level Apache tuning last.

## Contents

1. [Request rules](#1-request-rules)
2. [Connection pools](#2-connection-pools)
3. [Web starter](#3-web-starter)
4. [Resilience4j](#4-resilience4j)
5. [Actuator](#5-actuator)
6. [Global switches](#6-global-switches)
7. [Advanced Apache tuning](#7-advanced-apache-tuning)
8. [Legacy settings](#8-legacy-settings)

## Typical configuration

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
      default-connection-config:
        connect-timeout: PT1S
        socket-timeout: PT30S

    request-config:
      '[default]':
        connection-request-timeout: PT1S
        response-timeout: PT10S
        retry-config:
          max-retries: 2
          retry-interval: PT1S

      '[GET https://api.example.com/.*]':
        response-timeout: PT30S

      '[GET https://api.example.com/basic-auth/.*]':
        credentials: user:password
```

---

## 1. Request rules

`spring.httpclient5.request-config` is a map of regex rules applied per exchange.

### Rule key format

- A rule key is a Java regex matched against `METHOD + " " + absolute URI` (full match).
- Use `'[default]'` for the base rule.
- All matching rules are merged in **lexicographic key order**; the last value wins.
- `${...}` placeholders are resolved at startup, in keys and values.

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        response-timeout: PT10S
      '[GET https://api.example.com/.*]':
        response-timeout: PT30S
```

### Common Apache `RequestConfig` properties

Replace `<rule>` with your rule key.

> Defaults below are the effective values with the starter's **zero-conf rule**. If you declare your
> own `[default]` entry, values you do not set keep the Apache `RequestConfig` defaults
> (connection-request timeout 3 min, response timeout unset).

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.request-config.<rule>.connection-request-timeout` | `PT3S` (zero-conf) | Time to wait for a pooled connection |
| `spring.httpclient5.request-config.<rule>.response-timeout` | `PT30S` (zero-conf) | Time to wait for the response |
| `spring.httpclient5.request-config.<rule>.connection-keep-alive` | `PT3M` | Keep-alive duration |
| `spring.httpclient5.request-config.<rule>.redirects-enabled` | `true` | Follow redirects |
| `spring.httpclient5.request-config.<rule>.max-redirects` | `50` | Maximum redirects |
| `spring.httpclient5.request-config.<rule>.authentication-enabled` | `true` | Enable authentication |
| `spring.httpclient5.request-config.<rule>.content-compression-enabled` | `true` | Enable content compression |
| `spring.httpclient5.request-config.<rule>.protocol-upgrade-enabled` | `true` | Enable protocol upgrade (H2) |
| `spring.httpclient5.request-config.<rule>.hard-cancellation-enabled` | `true` | Enable hard cancellation |
| `spring.httpclient5.request-config.<rule>.proxy` | unset | Proxy URI, e.g. `http://localhost:3128` |
| `spring.httpclient5.request-config.<rule>.cookie-spec` | unset | Cookie policy name |
| `spring.httpclient5.request-config.<rule>.connect-timeout` | unset | **Deprecated** in Apache; use the pool connection config instead |

Any other `RequestConfig.Builder` property is also bindable; see
[Apache HttpClient Javadoc](https://hc.apache.org/httpcomponents-client-5.6.x/current/httpclient5/apidocs/).

### Credentials

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.request-config.<rule>.credentials` | unset | `user:password`, `BASIC(user:password)` or `PREEMPTIVE(user:password)` |

- `BASIC(...)` and the plain form use challenge-based basic authentication.
- `PREEMPTIVE(...)` sends `Authorization` immediately and keeps an existing header if present.
- The first `:` separates user and password; the password may contain `:`.

### Transport retries

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.request-config.<rule>.retry-config.max-retries` | `0` | Retry count for transport-level failures (`429`, `503`, idempotent I/O errors) |
| `spring.httpclient5.request-config.<rule>.retry-config.retry-interval` | `PT1S` | Delay between transport retries |

### Interceptors and request context

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.request-config.<rule>.interceptors` | `{}` | Map `interceptor-id` → enabled |
| `spring.httpclient5.request-config.<rule>.custom-request-context` | `{}` | Attributes added to the request `HttpContext` |

```yaml
'[default]':
  interceptors:
    '[trace]': true
  custom-request-context:
    '[tenant]': acme
```

An interceptor is a bean extending `ActivableHttpInterceptor`; its id is the map key.

### Resilience4j per rule

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.request-config.<rule>.error-management.circuit-name` | `default` | Resilience4j circuit name |
| `spring.httpclient5.request-config.<rule>.error-management.max-attempts` | `1` | Retry attempts (`1` = no retry) |
| `spring.httpclient5.request-config.<rule>.error-management.wait-duration` | `0` | Retry wait, in milliseconds |
| `spring.httpclient5.request-config.<rule>.error-management.broken-circuit-action` | `error` | `error` or an HTTP status code |

See [Resilience4j](#4-resilience4j) for the instance configuration.

### Header propagation per rule (web starter)

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.request-config.<rule>.headers-propagation.enabled` | unset (disabled) | Enable propagation for this rule |
| `spring.httpclient5.request-config.<rule>.headers-propagation.down` | `[]` | Regex list: incoming request headers forwarded to outgoing calls |
| `spring.httpclient5.request-config.<rule>.headers-propagation.up` | `[]` | Regex list: outgoing response headers copied to the incoming response |
| `spring.httpclient5.request-config.<rule>.headers-propagation.add` | `{}` | Static headers added to outgoing calls |
| `spring.httpclient5.request-config.<rule>.headers-propagation.remove` | `[]` | Headers removed from outgoing calls |

---

## 2. Connection pools

`pool` is the synchronous pool; `async-pool` is the asynchronous pool. Both use Apache
connection-manager builders; the asynchronous builder has no `default-socket-config` (use
`default-connection-config.socket-timeout` instead).

### Common pool settings

#### Synchronous pool

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.pool.max-conn-total` | `0` (unlimited) | Maximum total connections |
| `spring.httpclient5.pool.max-connper-route` | `50` | Maximum connections per route |
| `spring.httpclient5.pool.dns-resolver` | `system` | `system`, `in_memory`, or a `DnsResolver` class name |
| `spring.httpclient5.pool.conn-pool-policy` | `LIFO` | Connection reuse policy (`LIFO` / `FIFO`) |
| `spring.httpclient5.pool.connection-time-to-live` | unset | Pool-level connection TTL |
| `spring.httpclient5.pool.pool-concurrency-policy` | `LAX` | `STRICT` or `LAX` |
| `spring.httpclient5.pool.connection-idle-timeout` | `PT30S` | Idle connection timeout used by the cleaner |

> The `max-connper-route` spelling is historical; `max-conn-per-route` also binds.
> The `50` / `LAX` zero-conf values are skipped if either pool declares `max-connper-route`.

#### Asynchronous pool

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.async-pool.max-conn-total` | `0` (unlimited) | Maximum total connections |
| `spring.httpclient5.async-pool.max-connper-route` | `50` | Maximum connections per route |
| `spring.httpclient5.async-pool.dns-resolver` | `system` | `system`, `in_memory`, or a `DnsResolver` class name |
| `spring.httpclient5.async-pool.conn-pool-policy` | `LIFO` | Connection reuse policy (`LIFO` / `FIFO`) |
| `spring.httpclient5.async-pool.connection-time-to-live` | unset | Pool-level connection TTL |
| `spring.httpclient5.async-pool.pool-concurrency-policy` | `LAX` | `STRICT` or `LAX` |
| `spring.httpclient5.async-pool.connection-idle-timeout` | `PT30S` | Idle connection timeout used by the cleaner |

### Connection configuration

Applied to routes without a specific host entry. The starter always defaults `connect-timeout` to
3 s; `socket-timeout`, `time-to-live` and `validate-after-inactivity` are filled by the zero-conf
rule. If you declare the block yourself, omitted socket/TTL/validation values stay unset and Apache
falls back to its own defaults.

#### Synchronous pool

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.pool.default-connection-config.connect-timeout` | `PT3S` | TCP connect timeout |
| `spring.httpclient5.pool.default-connection-config.socket-timeout` | `PT30S` | Socket read timeout |
| `spring.httpclient5.pool.default-connection-config.time-to-live` | `PT120S` | Connection time-to-live |
| `spring.httpclient5.pool.default-connection-config.validate-after-inactivity` | `PT20S` | Re-validate idle connections |
| `spring.httpclient5.pool.default-connection-config.idle-timeout` | unset | Idle timeout |

#### Asynchronous pool

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.async-pool.default-connection-config.connect-timeout` | `PT3S` | TCP connect timeout |
| `spring.httpclient5.async-pool.default-connection-config.socket-timeout` | `PT30S` | Socket read timeout |
| `spring.httpclient5.async-pool.default-connection-config.time-to-live` | `PT120S` | Connection time-to-live |
| `spring.httpclient5.async-pool.default-connection-config.validate-after-inactivity` | `PT20S` | Re-validate idle connections |
| `spring.httpclient5.async-pool.default-connection-config.idle-timeout` | unset | Idle timeout |

### Socket configuration (synchronous pool only)

`so-timeout` is the zero-conf value; the other entries are Apache `SocketConfig` defaults.

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.pool.default-socket-config.so-timeout` | `PT30S` | `SO_TIMEOUT` |
| `spring.httpclient5.pool.default-socket-config.so-keepalive` | `true` | `SO_KEEPALIVE` |
| `spring.httpclient5.pool.default-socket-config.tcp-no-delay` | `true` | `TCP_NODELAY` |
| `spring.httpclient5.pool.default-socket-config.so-reuse-address` | `false` | `SO_REUSEADDR` |
| `spring.httpclient5.pool.default-socket-config.so-linger` | `-1` | `SO_LINGER` |
| `spring.httpclient5.pool.default-socket-config.snd-buf-size` | `0` | Send buffer size |
| `spring.httpclient5.pool.default-socket-config.rcv-buf-size` | `0` | Receive buffer size |
| `spring.httpclient5.pool.default-socket-config.backlog-size` | `0` | Backlog size |
| `spring.httpclient5.pool.default-socket-config.tcp-keep-idle` | `5` | TCP keep-alive idle |
| `spring.httpclient5.pool.default-socket-config.tcp-keep-interval` | `5` | TCP keep-alive interval |
| `spring.httpclient5.pool.default-socket-config.tcp-keep-count` | `3` | TCP keep-alive count |

### Host-specific configuration

Keys are host URIs. Placeholders are expanded and default ports are normalized
(`http` → `:80`, `https` → `:443`).

| Full key | Description |
|---|---|
| `spring.httpclient5.pool.host-config.<host>.connect-timeout` | Connect timeout for this host |
| `spring.httpclient5.pool.host-config.<host>.socket-timeout` | Socket timeout for this host |
| `spring.httpclient5.pool.host-config.<host>.time-to-live` | Connection TTL for this host |
| `spring.httpclient5.pool.host-config.<host>.validate-after-inactivity` | Re-validation delay for this host |
| `spring.httpclient5.pool.host-config.<host>.max-connections` | Max-per-route for this host |
| `spring.httpclient5.async-pool.host-config.<host>.connect-timeout` | Same, asynchronous pool |
| `spring.httpclient5.async-pool.host-config.<host>.socket-timeout` | Same, asynchronous pool |
| `spring.httpclient5.async-pool.host-config.<host>.time-to-live` | Same, asynchronous pool |
| `spring.httpclient5.async-pool.host-config.<host>.validate-after-inactivity` | Same, asynchronous pool |
| `spring.httpclient5.async-pool.host-config.<host>.max-connections` | Same, asynchronous pool |

```yaml
spring:
  httpclient5:
    pool:
      host-config:
        '[https://api.example.com]':
          connect-timeout: PT2S
          socket-timeout: PT45S
          max-connections: 20
```

---

## 3. Web starter

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.web.headers-filter.enabled` | `true` | Register the servlet filter that collects/serves headers |
| `spring.httpclient5.web.headers-propagation.enabled` | `true` | Enable the header-propagation auto-configuration |
| `spring.httpclient5.web.async-decorator.enabled` | `true` | Propagate the request context to `@Async` tasks |
| `spring.httpclient5.web.rest-template.enabled` | `true` | Customize Boot's `RestTemplateBuilder` with this `HttpClient` |

Header rules are configured per request rule; see
[header propagation](#header-propagation-per-rule-web-starter).

---

## 4. Resilience4j

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.core.resilience4j.enabled` | `true` | Enable the Resilience4j handlers |

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
- For any name other than `default`, a `RateLimiter` with the **same name** is also used.
- HTTP 5xx and exceptions are failures.
- `broken-circuit-action` accepts `error` (throws) or an HTTP status code.

---

## 5. Actuator

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.core.actuator.enabled` | `true` | Enable request observations and pool metrics |

Meters registered by the module:

| Meter | Type | Tags |
|---|---|---|
| `http.client.request.duration` | timer | `http.request.method`, `server.address`, `server.port`, `http.response.status_code`, `error.type`, `outcome` |
| `httpcomponents.httpclient.pool.total.max` | gauge | `httpclient` |
| `httpcomponents.httpclient.pool.total.connections` | gauge | `httpclient`, `state=available\|leased` |
| `httpcomponents.httpclient.pool.total.pending` | gauge | `httpclient` |
| `httpcomponents.httpclient.pool.route.max.default` | gauge | `httpclient` |

The `httpclient` tag is `httpclient5.pool` for the synchronous pool and `httpclient5.async-pool`
for the asynchronous pool. See [Actuator metrics](actuator-metrics.md) for Prometheus names,
queries and per-line name differences.

---

## 6. Global switches

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.core.enabled` | `true` | Enable the core auto-configuration |
| `spring.httpclient5.autoconfig` | `true` | Apply zero-conf pool/request defaults |
| `spring.httpclient5.user-agent` | unset | `User-Agent` header for both clients |

Zero-conf defaults (when `autoconfig=true`):

| Setting | Value |
|---|---|
| Request connection-request timeout | 3 s |
| Request response timeout | 30 s |
| Max connections per route (both pools) | 50 |
| Pool concurrency policy | `LAX` |
| Connect timeout | 3 s |
| Socket timeout | 30 s |
| Connection TTL | 120 s |
| Validate after inactivity | 20 s |
| SO_TIMEOUT (synchronous pool) | 30 s |
| Idle connection timeout | 30 s |
| Max total connections | unlimited |

---

## 7. Advanced Apache tuning

These settings are rarely needed; the defaults are usually correct.

### HTTP/1

| Full key | Apache default | Description |
|---|---|---|
| `spring.httpclient5.http1.buffer-size` | `32768` | Internal buffer size |
| `spring.httpclient5.http1.chunk-size-hint` | `-1` | Chunk size hint |
| `spring.httpclient5.http1.wait-for-continue-timeout` | unset | Timeout for `Expect: 100-continue` |
| `spring.httpclient5.http1.max-line-length` | `8192` | Maximum line length |
| `spring.httpclient5.http1.max-header-count` | `100` | Maximum header count |
| `spring.httpclient5.http1.max-empty-line-count` | `10` | Maximum tolerated empty lines |

### Character coding

| Full key | Description |
|---|---|
| `spring.httpclient5.char-coding.charset` | Character set |
| `spring.httpclient5.char-coding.malformed-input-action` | Action on malformed input |
| `spring.httpclient5.char-coding.unmappable-input-action` | Action on unmappable input |

### I/O reactor (asynchronous client)

| Full key | Apache default | Description |
|---|---|---|
| `spring.httpclient5.io-reactor.select-interval` | `PT1S` | Selector interval |
| `spring.httpclient5.io-reactor.io-thread-count` | derived from CPUs | I/O thread count |
| `spring.httpclient5.io-reactor.so-timeout` | `0` | Socket timeout |
| `spring.httpclient5.io-reactor.so-keepalive` | `true` | `SO_KEEPALIVE` |
| `spring.httpclient5.io-reactor.tcp-no-delay` | `true` | `TCP_NODELAY` |
| `spring.httpclient5.io-reactor.snd-buf-size` | `0` | Send buffer size |
| `spring.httpclient5.io-reactor.rcv-buf-size` | `0` | Receive buffer size |
| `spring.httpclient5.io-reactor.backlog-size` | `0` | Backlog size |

---

## 8. Legacy settings

| Full key | Default | Description |
|---|---|---|
| `spring.httpclient5.jmx.domain` | `default` | Legacy field, not wired to JMX in 2.x |
| `spring.httpclient5.jmx.metric-name-strategy` | `HOST_AND_METHOD` | Legacy field, not used in 2.x |

Use the Actuator module and Micrometer for monitoring instead.
