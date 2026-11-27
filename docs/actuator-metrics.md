# Actuator metrics

`spring-boot-starter-httpclient5-actuator` instruments Apache HttpClient 5 with Micrometer.
It adds:

- one **request observation** (timer, and a span when tracing is enabled) for the synchronous
  and the asynchronous clients;
- **connection-pool gauges** for each connection manager (sync and async).

No custom endpoint or health indicator is added: all meters go through the standard Spring Boot
Actuator / Micrometer pipeline (`/actuator/metrics`, Prometheus, OTLP, ...).

## Setup

```xml
<dependency>
    <groupId>io.github.springboot-addons</groupId>
    <artifactId>spring-boot-starter-httpclient5-actuator</artifactId>
    <version>1.3.1</version>
</dependency>

<!-- Any Micrometer registry: Prometheus, OTLP, ... -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

The actuator module already depends on `spring-boot-starter-actuator` and `micrometer-core`.
The auto-configuration (`HttpClientActuatorAutoConfiguration`) is active by default. It can be
disabled with:

```yaml
spring:
  httpclient5:
    core:
      actuator:
        enabled: false
```

This removes both the request observation and the pool gauges. The same property written as a
flat key is `spring.httpclient5.core.actuator.enabled`.

To browse the meters over HTTP:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## How it is wired

- An `ObservationExecChainHandler` bean (ordered `LOWEST_PRECEDENCE`) is registered as the first
  execution interceptor on both `CloseableHttpClient` and `CloseableHttpAsyncClient`. The handler
  implements both `ExecChainHandler` and `AsyncExecChainHandler`, so the same bean instruments
  both clients.
- Two `PoolingHttpClientConnectionManagerMetricsBinder` beans are declared. They implement
  `MeterBinder`, so Spring Boot binds them automatically to every `MeterRegistry`:
  - synchronous pool, binder name `httpclient5.pool` → tag `httpclient=httpclient5.pool`;
  - asynchronous pool, binder name `httpclient5.async-pool` → tag
    `httpclient=httpclient5.async-pool`.

`httpclient5.pool` and `httpclient5.async-pool` are **tag values**, not metric name prefixes.

## Request metric

| Micrometer name | Type | Base unit |
|---|---|---|
| `httpcomponents.httpclient.request` | `Timer` | seconds |

A normal client execution records one timer sample. The legacy convention carries these tags:

| Tag | Values | Description |
|---|---|---|
| `method` | `GET`, `POST`, ... or `UNKNOWN` | HTTP method |
| `uri` | URI template, request URI or `UNKNOWN` | Request URI. Taken from the `micrometer.uri.template` request attribute when present. |
| `status` | `200`, `404`, ..., `IO_ERROR` or `CLIENT_ERROR` | Response status; `IO_ERROR` for I/O/HTTP/runtime errors, `CLIENT_ERROR` when no response is available |
| `outcome` | `INFORMATIONAL`, `SUCCESS`, `REDIRECTION`, `CLIENT_ERROR`, `SERVER_ERROR`, `UNKNOWN` | Status family |
| `exception` | exception simple name or `none` | Exception class when the exchange failed, `none` otherwise |
| `target.host` | host name or `UNKNOWN` | Target host |
| `target.port` | port number or `UNKNOWN` | Target port |
| `target.scheme` | `http`, `https` or `UNKNOWN` | Target scheme |

The `uri` tag is part of the timer on this line. To keep cardinality under control, set the
`micrometer.uri.template` request context attribute (or a URI mapper) instead of relying on raw
URIs.

When a Micrometer tracing bridge is on the classpath, the same observation also creates a client
span. Its contextual name is the HTTP method (`GET`, `POST`, ...) or `HTTP UNKNOWN` when the
method is unknown.

Percentiles and histograms follow the standard Boot properties (metric name with dots):

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        httpcomponents.httpclient.request: true
      slo:
        httpcomponents.httpclient.request: 100ms,500ms,1s
```

## Connection-pool gauges

Both pools expose the same four gauge names (five series). The only difference is the `httpclient`
tag value.

| Micrometer name | Type | Tags | Description |
|---|---|---|---|
| `httpcomponents.httpclient.pool.total.max` | gauge | `httpclient` | Configured maximum connections for all routes (the pool's `max-conn-total`). `0` means unlimited. |
| `httpcomponents.httpclient.pool.total.connections` | gauge | `httpclient`, `state=available` | Persistent connections currently available in the pool |
| `httpcomponents.httpclient.pool.total.connections` | gauge | `httpclient`, `state=leased` | Persistent connections currently checked out |
| `httpcomponents.httpclient.pool.total.pending` | gauge | `httpclient` | Connection requests waiting for a free connection |
| `httpcomponents.httpclient.pool.route.max.default` | gauge | `httpclient` | Default maximum connections per route (the pool's `max-connper-route`) |

Tag values:

| `httpclient` tag | Connection manager | Related configuration |
|---|---|---|
| `httpclient5.pool` | `PoolingHttpClientConnectionManager` | `spring.httpclient5.pool` |
| `httpclient5.async-pool` | `PoolingAsyncClientConnectionManager` | `spring.httpclient5.async-pool` |

With the zero-configuration defaults, `total.max` is `0` (unlimited) and `route.max.default` is
`50`. A `total.pending` value that stays above zero means the pool is saturated: requests are
blocked waiting for a connection.

## Prometheus names

The Prometheus registry converts dots to underscores and appends the base unit for timers.

| Micrometer name | Prometheus name |
|---|---|
| `httpcomponents.httpclient.request` | `httpcomponents_httpclient_request_seconds` |
| `httpcomponents.httpclient.pool.total.max` | `httpcomponents_httpclient_pool_total_max` |
| `httpcomponents.httpclient.pool.total.connections` | `httpcomponents_httpclient_pool_total_connections` |
| `httpcomponents.httpclient.pool.total.pending` | `httpcomponents_httpclient_pool_total_pending` |
| `httpcomponents.httpclient.pool.route.max.default` | `httpcomponents_httpclient_pool_route_max_default` |

Tag keys become `httpclient`, `state`, `method`, `uri`, `status`, `outcome`, `exception`,
`target_host`, `target_port` and `target_scheme`.

## Queries

Actuator endpoint (the `tag` parameter can be repeated):

```bash
# Request timer, all tags
curl -s 'http://localhost:8080/actuator/metrics/httpcomponents.httpclient.request'

# Leased connections of the synchronous pool
curl -s 'http://localhost:8080/actuator/metrics/httpcomponents.httpclient.pool.total.connections?tag=httpclient:httpclient5.pool&tag=state:leased'

# Requests sent to api.example.com
curl -s 'http://localhost:8080/actuator/metrics/httpcomponents.httpclient.request?tag=target.host:api.example.com'
```

PromQL examples:

```promql
# Requests per second by target and status
sum by (target_host, status) (
  rate(httpcomponents_httpclient_request_seconds_count[5m])
)

# p95 latency by target (requires percentiles-histogram)
histogram_quantile(0.95,
  sum by (le, target_host) (
    rate(httpcomponents_httpclient_request_seconds_bucket[5m])
  )
)

# Leased connection ratio per pool (clamp avoids a division by zero when max = 0)
sum by (httpclient) (httpcomponents_httpclient_pool_total_connections{state="leased"})
  / clamp_min(sum by (httpclient) (httpcomponents_httpclient_pool_total_max), 1)

# Requests waiting for a connection
sum by (httpclient) (httpcomponents_httpclient_pool_total_pending)
```

Individual meters can be disabled with the standard Boot property. Bracket notation keeps the
dotted metric name intact in YAML:

```yaml
management:
  metrics:
    enable:
      '[httpcomponents.httpclient.pool.total.pending]': false
```

## Metric names per starter line

This branch is the 1.3.x line: the sections above describe its legacy convention. The table
summarizes the other lines.

Pool gauges are identical on every branch. The request metric depends on the Micrometer version
used by the Spring Boot line:

| Starter line | Spring Boot | Micrometer | Request metric | Tags |
|---|---|---|---|---|
| 2.2.x | 4.2.x | 1.18.x | `http.client.request.duration` | `http.request.method`, `server.address`, `server.port`, `http.response.status_code`, `error.type`, `outcome` |
| 2.1.x | 4.1.x | 1.17.x | `http.client.request.duration` | same |
| 2.0.x | 4.0.x | 1.16.x | `http.client.request.duration` | same |
| 1.3.x | 3.5.x | 1.15.x | `httpcomponents.httpclient.request` | `method`, `uri`, `status`, `outcome`, `exception`, `target.host`, `target.port`, `target.scheme` |

On the 3.5 line, Prometheus therefore exposes
`httpcomponents_httpclient_request_seconds`. On Boot 4.0 and later, the starter explicitly uses
Micrometer's OpenTelemetry convention, which produces the `http.client.request.duration`
semantic-convention name.
