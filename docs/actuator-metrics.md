# Actuator metrics

`spring-boot-starter-httpclient5-actuator` instruments Apache HttpClient 5 with Micrometer.
It adds:

- one **request observation** (timer, and a span when tracing is enabled) for the synchronous
  and the asynchronous clients;
- **connection-pool gauges** for each connection manager (sync and async);
- **adaptive timeout gauges** when the optional adaptive response timeout policy is enabled.

No custom endpoint or health indicator is added: all meters go through the standard Spring Boot
Actuator / Micrometer pipeline (`/actuator/metrics`, Prometheus, OTLP, ...).

## Setup

```xml
<dependency>
    <groupId>io.github.springboot-addons</groupId>
    <artifactId>spring-boot-starter-httpclient5-actuator</artifactId>
    <version>2.2.1</version>
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
| `http.client.request.duration` | `Timer` | seconds |

A normal client execution records one timer sample. The timer carries low-cardinality tags only:

| Tag | Values | Description |
|---|---|---|
| `http.request.method` | `GET`, `POST`, ... or `_OTHER` | HTTP method; non-standard methods are grouped under `_OTHER` |
| `server.address` | host name or `UNKNOWN` | target host |
| `server.port` | port number or `-1` | target port |
| `http.response.status_code` | `200`, `404`, ... or `0` | response status; `0` when no response was received (connect error, timeout) |
| `error.type` | exception simple name or `none` | exception class when the exchange failed, `none` otherwise |
| `outcome` | `INFORMATIONAL`, `SUCCESS`, `REDIRECTION`, `CLIENT_ERROR`, `SERVER_ERROR`, `UNKNOWN` | status family |

`url.full` is a **high-cardinality** key: it is attached to spans, never to the timer, so metric
cardinality stays bounded.

When a Micrometer tracing bridge is on the classpath, the same observation also creates a client
span. Its contextual name is the HTTP method (`GET`, `POST`, ...) or `HTTP` when the method is
unknown.

Percentiles and histograms follow the standard Boot properties (metric name with dots):

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.client.request.duration: true
      slo:
        http.client.request.duration: 100ms,500ms,1s
```

## Adaptive timeout

When the adaptive response timeout policy is active, the actuator module exposes the state of the
policy in addition to the standard request timer:

| Micrometer name | Type | Tags | Description |
|---|---|---|---|
| `httpclient5.adaptive.timeout` | gauge | none | Current computed response timeout, in seconds. `NaN` until the policy is ready. |
| `httpclient5.adaptive.percentile` | gauge | none | Configured TTFB percentile used by the policy, in seconds |
| `httpclient5.adaptive.p50` | gauge | none | Median TTFB, in seconds |
| `httpclient5.adaptive.p90` | gauge | none | p90 TTFB, in seconds |
| `httpclient5.adaptive.p99` | gauge | none | p99 TTFB, in seconds |
| `httpclient5.adaptive.samples` | gauge | none | Number of samples used by the last computation |
| `httpclient5.adaptive.status` | gauge | none | `0=DISABLED`, `1=WARMING_UP`, `2=READY`, `3=STALE` |
| `httpclient5.adaptive.ready` | gauge | none | `1` when the policy is `READY`, `0` otherwise |
| `httpclient5.adaptive.stale` | gauge | none | `1` when the policy is `STALE`, `0` otherwise |
| `httpclient5.adaptive.timeouts` | function counter | none | Response timeouts observed by the policy |

The latency window is **global and bounded**: the global meters have no per-rule tag, so their
cardinality stays constant. All requests feed the window; the per-rule `adaptive-timeout.enabled`
flag only controls whether the computed value is applied to the matching requests. See the
[configuration guide](configuration-guide.md#adaptive-response-timeout) for the policy algorithm
and properties.

### Per-rule metrics

The following meters are registered for each configured rule. The `rule` tag value is the request
configuration key; cardinality is bounded by the number of configured rules.

| Micrometer name | Type | Tags | Description |
|---|---|---|---|
| `httpclient5.adaptive.rule.configured.timeout` | gauge | `rule` | Configured timeout, in seconds. `NaN` when unset. |
| `httpclient5.adaptive.rule.candidate.timeout` | gauge | `rule` | Computed adaptive candidate, in seconds |
| `httpclient5.adaptive.rule.effective.timeout` | gauge | `rule` | Effective timeout actually applied, in seconds |
| `httpclient5.adaptive.rule.differs` | gauge | `rule` | `1` when the candidate differs from the configured timeout |
| `httpclient5.adaptive.rule.requests` | function counter | `rule` | Requests resolved for the rule |
| `httpclient5.adaptive.rule.timeouts` | function counter | `rule` | Response timeouts observed for the rule |
| `httpclient5.adaptive.rule.failures` | function counter | `rule` | Non-timeout failures observed for the rule |

In `dry-run` mode the candidate is computed and exposed, but the effective timeout remains the
configured one (`differs=1`, `applied=false` in the endpoint).

### Adaptive timeout diagnostics endpoint

The actuator module registers a read-only endpoint:

```text
GET /actuator/httpclient5-adaptive-timeout
```

Expose it like any other endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,httpclient5-adaptive-timeout
```

The response contains:

| Field | Description |
|---|---|
| `status` | `DISABLED`, `WARMING_UP`, `READY` or `STALE` |
| `globalEnabled`, `dryRun`, `ready`, `stale` | Global switch, dry-run mode, readiness and stale-window flags |
| `lastComputedAt`, `ageMs` | Wall-clock time and age of the last computation |
| `configuration` | Global policy parameters and diagnostics settings |
| `current` | Computed timeout, configured percentile, p50/p90/p99, minimum observed TTFB, sample counts, timeout rate and warmup progress |
| `lastRequest` | Last resolved rule, effective timeout, timeout flag and timestamp (no raw URI) |
| `rules.enabled` / `rules.different` | Number of rules that enable the policy / whose candidate differs from the configuration |
| `rules.items[]` | Per rule: `rule`, `adaptiveEnabled`, `configuredTimeoutMs`, `configuredSource`, `candidateTimeoutMs`, `effectiveTimeoutMs`, `deltaMs`, `differs`, `applied`, `requests`, `timeouts`, `failures`, `lastSeenAt`, `lastEffectiveTimeoutMs`, `lastTimedOut` |

`configuredSource` is one of `rule` (the rule declares `response-timeout`), `default` (it inherits
the `[default]` rule value) or `unset`. The comparison uses the declared values; it does not resolve
the full regex merge for a concrete URI.

Example:

```json
{
  "status": "READY",
  "globalEnabled": true,
  "dryRun": false,
  "ready": true,
  "stale": false,
  "lastComputedAt": "2026-11-27T13:00:00Z",
  "ageMs": 1200,
  "current": {
    "timeoutMs": 45,
    "percentileMs": 30,
    "p50Ms": 12,
    "p90Ms": 24,
    "p99Ms": 30,
    "minObservedMs": 10,
    "samples": 1234,
    "totalSamples": 5000,
    "timeouts": 12,
    "timeoutRate": 0.0024,
    "warmupProgress": 1.0
  },
  "lastRequest": {
    "rule": "GET https://api.example.com/.*",
    "effectiveTimeoutMs": 45,
    "timedOut": false,
    "at": "2026-11-27T13:00:02Z"
  },
  "rules": {
    "enabled": 2,
    "different": 1,
    "items": [
      {
        "rule": "GET https://api.example.com/.*",
        "adaptiveEnabled": true,
        "configuredTimeoutMs": 10000,
        "configuredSource": "rule",
        "candidateTimeoutMs": 45,
        "effectiveTimeoutMs": 45,
        "deltaMs": -9955,
        "differs": true,
        "applied": true,
        "requests": 1200,
        "timeouts": 3,
        "failures": 1,
        "lastSeenAt": "2026-11-27T13:00:02Z",
        "lastEffectiveTimeoutMs": 45,
        "lastTimedOut": false
      }
    ]
  }
}
```

### Adaptive timeout debug log

When `spring.httpclient5.adaptive-timeout.diagnostics.log-enabled=true` (default), a scheduled task
logs, at DEBUG, only the rules whose candidate differs from the configured one:

```text
Adaptive timeout differs (dryRun=false, samples=1234, p99=30ms, stale=false): rule='GET https://api.example.com/.*' configured=10000ms source=rule candidate=45ms effective=45ms delta=-9955ms applied=true requests=1200 timeouts=3
```

The same task emits INFO events when the computed timeout changes by more than
`change-log-threshold-percent` or when the policy becomes `STALE`:

```text
Adaptive response timeout changed by 25.0% (40ms -> 50ms, p99=35ms, samples=1234)
Adaptive response timeout policy became STALE (last computed 120000 ms ago)
```

Enable the logger:

```yaml
logging:
  level:
    io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutDiagnosticsLogger: DEBUG
```

| Property | Default | Description |
|---|---|---|
| `spring.httpclient5.adaptive-timeout.diagnostics.log-enabled` | `true` | Log differing rules at DEBUG |
| `spring.httpclient5.adaptive-timeout.diagnostics.change-log-enabled` | `true` | Log timeout changes and STALE transitions at INFO |
| `spring.httpclient5.adaptive-timeout.diagnostics.change-log-threshold-percent` | `20.0` | Relative change required for an INFO timeout-change event |
| `spring.httpclient5.adaptive-timeout.diagnostics.log-interval` | `PT1M` | Period between two diagnostic runs (ISO-8601) |
| `spring.httpclient5.adaptive-timeout.diagnostics.log-max-rules` | `20` | Maximum differing rules logged per period; the rest is summarized |

The DEBUG task logs nothing when the policy is not ready or when no rule differs. The scheduled
task is registered when either `log-enabled` or `change-log-enabled` is true. Set both to `false`
to remove it entirely.

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
| `http.client.request.duration` | `http_client_request_duration_seconds` |
| `httpcomponents.httpclient.pool.total.max` | `httpcomponents_httpclient_pool_total_max` |
| `httpcomponents.httpclient.pool.total.connections` | `httpcomponents_httpclient_pool_total_connections` |
| `httpcomponents.httpclient.pool.total.pending` | `httpcomponents_httpclient_pool_total_pending` |
| `httpcomponents.httpclient.pool.route.max.default` | `httpcomponents_httpclient_pool_route_max_default` |
| `httpclient5.adaptive.timeout` | `httpclient5_adaptive_timeout_seconds` |
| `httpclient5.adaptive.percentile` | `httpclient5_adaptive_percentile_seconds` |
| `httpclient5.adaptive.p50` | `httpclient5_adaptive_p50_seconds` |
| `httpclient5.adaptive.p90` | `httpclient5_adaptive_p90_seconds` |
| `httpclient5.adaptive.p99` | `httpclient5_adaptive_p99_seconds` |
| `httpclient5.adaptive.samples` | `httpclient5_adaptive_samples` |
| `httpclient5.adaptive.status` | `httpclient5_adaptive_status` |
| `httpclient5.adaptive.ready` | `httpclient5_adaptive_ready` |
| `httpclient5.adaptive.stale` | `httpclient5_adaptive_stale` |
| `httpclient5.adaptive.timeouts` | `httpclient5_adaptive_timeouts_total` |
| `httpclient5.adaptive.rule.configured.timeout` | `httpclient5_adaptive_rule_configured_timeout_seconds` |
| `httpclient5.adaptive.rule.candidate.timeout` | `httpclient5_adaptive_rule_candidate_timeout_seconds` |
| `httpclient5.adaptive.rule.effective.timeout` | `httpclient5_adaptive_rule_effective_timeout_seconds` |
| `httpclient5.adaptive.rule.differs` | `httpclient5_adaptive_rule_differs` |
| `httpclient5.adaptive.rule.requests` | `httpclient5_adaptive_rule_requests_total` |
| `httpclient5.adaptive.rule.timeouts` | `httpclient5_adaptive_rule_timeouts_total` |
| `httpclient5.adaptive.rule.failures` | `httpclient5_adaptive_rule_failures_total` |

Tag keys become `httpclient`, `state`, `rule`, `http_request_method`, `server_address`,
`server_port`, `http_response_status_code`, `error_type` and `outcome`.

## Queries

Actuator endpoint (the `tag` parameter can be repeated):

```bash
# Request timer, all tags
curl -s 'http://localhost:8080/actuator/metrics/http.client.request.duration'

# Leased connections of the synchronous pool
curl -s 'http://localhost:8080/actuator/metrics/httpcomponents.httpclient.pool.total.connections?tag=httpclient:httpclient5.pool&tag=state:leased'

# Requests sent to api.example.com
curl -s 'http://localhost:8080/actuator/metrics/http.client.request.duration?tag=server.address:api.example.com'
```

PromQL examples:

```promql
# Requests per second by target and status
sum by (server_address, http_response_status_code) (
  rate(http_client_request_duration_seconds_count[5m])
)

# p95 latency by target (requires percentiles-histogram)
histogram_quantile(0.95,
  sum by (le, server_address) (
    rate(http_client_request_duration_seconds_bucket[5m])
  )
)

# Leased connection ratio per pool (clamp avoids a division by zero when max = 0)
sum by (httpclient) (httpcomponents_httpclient_pool_total_connections{state="leased"})
  / clamp_min(sum by (httpclient) (httpcomponents_httpclient_pool_total_max), 1)

# Requests waiting for a connection
sum by (httpclient) (httpcomponents_httpclient_pool_total_pending)

# Current adaptive response timeout
httpclient5_adaptive_timeout_seconds

# Response timeouts observed by the adaptive policy
rate(httpclient5_adaptive_timeouts_total[5m])

# Rules whose candidate differs from the configuration
httpclient5_adaptive_rule_differs == 1

# Configured vs effective timeout per rule
httpclient5_adaptive_rule_configured_timeout_seconds
httpclient5_adaptive_rule_effective_timeout_seconds

# Timeouts per rule
sum by (rule) (rate(httpclient5_adaptive_rule_timeouts_total[5m]))

# Alerting signals: STALE=3, WARMING_UP=1
httpclient5_adaptive_status == 3
httpclient5_adaptive_status == 1
```

Prometheus alerting examples (add an appropriate `for:` duration):

```promql
# Policy STALE for 10 minutes
httpclient5_adaptive_status == 3

# Policy warming up for 30 minutes (the warmup may be too slow for the traffic)
httpclient5_adaptive_status == 1
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

The adaptive timeout meters (`httpclient5.adaptive.*`) are specific to the 2.2.x line (`develop`).
They are not backported to the 2.1.x, 2.0.x or 1.3.x lines.
