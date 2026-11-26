# AI agent quick reference

This file is a compact context for AI coding agents. The authoritative contribution, build and
release instructions are in [AGENTS.md](AGENTS.md); user documentation starts at
[README.md](README.md).

## Project snapshot

- Group: `io.github.springboot-addons`
- Current branch: `hc5-sb42` (Spring Boot 4.2.x)
- Current version: `2.2.2-SNAPSHOT`; last release: `2.2.1`
- Runtime target: Java 17; build validated with JDK 25
- Maven Wrapper: 3.9.11
- Public Maven repository: Maven Central

## Module map

| Module | Main entry points |
|---|---|
| `spring-boot-starter-httpclient5` | `HttpClient5AutoConfiguration`, `HttpClient5Config`, `HttpClientConfigurer`, `HttpAsyncClientConfigurer`, `RequestConfigProperties` |
| `spring-boot-starter-httpclient5-web` | `AsyncExecutorConfig`, `HeadersConfiguration`, `HeadersFilter`, `HeadersPropagationInterceptor`, `HttpClient5RestTemplateConfig` |
| `spring-boot-starter-httpclient5-actuator` | `HttpClientActuatorAutoConfiguration` |
| `spring-boot-starter-httpclient5-resilience4j` | `HttpClientResilience4jAutoConfiguration`, `ResilienceExecChainHandler` |

## Configuration in one minute

- Prefix: `spring.httpclient5`.
- `pool` / `async-pool`: Apache connection-manager builder properties plus `connection-idle-timeout`
  and `host-config`.
- `request-config`: map of `METHOD + " " + URI` regex rules merged in lexicographic order on top of
  `[default]`.
- Starter extensions per rule: `credentials`, `retry-config`, `interceptors`,
  `custom-request-context`, `error-management`, `headers-propagation`.
- Zero-conf defaults: 3 s connection-request, 30 s response, 50 max-per-route, LAX, 3 s connect,
  30 s socket, 120 s TTL, 30 s idle timeout.

See [docs/CONFIGURATION-ALL.md](docs/CONFIGURATION-ALL.md) for the full list.

## Invariants

1. Keep `java.version` / `maven.compiler.release` at **17**.
2. Keep `<proc>full</proc>` in the compiler plugin: JDK 23+ no longer discovers annotation
   processors from the classpath.
3. Keep the `--add-opens` flags in `.mvn/jvm.config`; do not reintroduce `JDK_JAVA_OPTIONS` in the
   release procedure.
4. Tests must stay self-contained: Testcontainers `go-httpbin`, never a private/external host.
5. The release is a single command and uses `MAVEN_GPG_PASSPHRASE`, `releaseProfiles`,
   `autoPublish` and automatic push.
6. Target Spring Boot versions per branch: `hc5-sb3` 3.5.x, `hc5-sb4` 4.0.x, `hc5-sb41` 4.1.x,
   `hc5-sb42` 4.2.x.

## Commands

```powershell
.\mvnw.cmd clean verify          # full build + Testcontainers tests (Docker required)
.\mvnw.cmd test -pl <module>     # one module
.\mvnw.cmd clean test-compile    # fast compile check
.\mvnw.cmd release:prepare release:perform
```

## Documentation map

- [README.md](README.md) – user overview and quick start
- [docs/configuration-guide.md](docs/configuration-guide.md) – configuration concepts
- [docs/CONFIGURATION-ALL.md](docs/CONFIGURATION-ALL.md) – property reference
- [docs/usage-example.md](docs/usage-example.md) – code examples
- [AGENTS.md](AGENTS.md) – contributor/build/release instructions
