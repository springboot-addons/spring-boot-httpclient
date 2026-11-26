# Configuration model

`spring-boot-httpclient5` exposes one configuration tree under `spring.httpclient5`. This page is a
short overview; see the [configuration guide](docs/configuration-guide.md) and the
[full reference](docs/CONFIGURATION-ALL.md) for details.

## Four layers

1. **Bootstrap flags** – enable/disable auto-configuration
   (`spring.httpclient5.core.enabled`, `.core.actuator.enabled`, `.core.resilience4j.enabled`,
   `.web.*.enabled`).
2. **Connection pools** – `spring.httpclient5.pool` (sync) and `spring.httpclient5.async-pool`
   (async): connections, timeouts, socket options, per-host overrides.
3. **Request rules** – `spring.httpclient5.request-config`: regex rules on `METHOD + " " + URI`
   that carry Apache `RequestConfig` values plus credentials, retries, interceptors, context and
   Resilience4j settings.
4. **Starter features** – header propagation, Actuator metrics and Resilience4j handlers.

## Zero-configuration defaults

| Setting | Value |
|---|---|
| Request connection-request timeout | 3 s |
| Request response timeout | 30 s |
| Max connections per route (sync and async) | 50 |
| Pool concurrency policy | `LAX` |
| Connect timeout | 3 s |
| Socket timeout | 30 s |
| Connection TTL | 120 s |
| Validate after inactivity | 20 s |
| SO_TIMEOUT (synchronous pool) | 30 s |
| Idle connection timeout | 30 s |
| Max total connections | unlimited |
| User-Agent | none |

Set `spring.httpclient5.autoconfig=false` to use Apache HttpClient defaults instead.

## Request rules

```yaml
spring:
  httpclient5:
    request-config:
      '[default]':
        response-timeout: PT10S
      '[GET https://api.example.com/.*]':
        response-timeout: PT30S
        credentials: user:password
      '[POST https://api.example.com/.*]':
        retry-config:
          max-retries: 2
          retry-interval: PT1S
```

- A rule key is a Java regex matched against `METHOD + " " + absolute URI` (full match).
- `[default]` is the base rule.
- All matching rules are merged in lexicographic key order; the last value wins.
- `${...}` placeholders are resolved in keys and values.
- Host configuration keys are normalized with default ports (`http` → `:80`, `https` → `:443`).

## Starter features

- **web**: bidirectional header propagation (`headers-propagation`), request-context propagation to
  `@Async` tasks, `RestTemplate` customization.
- **actuator**: request metrics and pool metrics (`httpclient5.pool`,
  `httpclient5.async-pool`).
- **resilience4j**: circuit breaker and, for non-`default` circuit names, a rate limiter with the
  same name.

## More documentation

- [README](README.md) – quick start and compatibility
- [Configuration guide](docs/configuration-guide.md) – concepts and examples
- [Configuration reference](docs/CONFIGURATION-ALL.md) – every property and default
- [Usage examples](docs/usage-example.md) – sync, async, fluent, `RestTemplate`, propagation
- [AGENTS.md](AGENTS.md) – build, test and release procedure
