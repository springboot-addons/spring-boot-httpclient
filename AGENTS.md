# AGENTS.md - spring-boot-httpclient5

## Project identity

Autonomous, public library published on:

- GitHub: https://github.com/springboot-addons/spring-boot-httpclient
- Maven Central: `io.github.springboot-addons:*`

The repository root is this directory (`spring-boot-httpclient`). It is **not** part of the
`rci-socle-api` repository anymore: do not reference, import or copy files from
`d:\workspace\rci-socle-api-sb4`.

Current version: `2.0.3-SNAPSHOT`. Last release: `2.0.2` (tag
`spring-boot-starter-httpclient5-parent-2.0.2`).

## Branches

Development happens on **`develop`**. The `hc5-*` branches are release/maintenance branches:
a change is merged or cherry-picked into them only when it must be released on that line.

| Branch | Role | Spring Boot | Starter line | Released |
|---|---|---|---|---|
| `develop` | default integration branch, all new work | 4.2.x | 2.2.x | never |
| `hc5-sb42` | release branch, 4.2 line | 4.2.x | 2.2.x | 2.2.x |
| `hc5-sb41` | maintenance branch, 4.1 line | 4.1.x | 2.1.x | 2.1.x |
| `hc5-sb4` | maintenance branch, 4.0 line | 4.0.x | 2.0.x | 2.0.x |
| `hc5-sb3` | maintenance branch, 3.5 line | 3.5.x | 1.3.x | 1.3.x |

Never merge `develop` into an older release branch: their Spring Boot, HttpClient and library
versions differ. Promote to `hc5-sb42` with a merge, and backport to the other lines with
`git cherry-pick -x` (see [Development workflow](#development-workflow)).

## Modules

| Module | Purpose |
|---|---|
| `spring-boot-starter-httpclient5` | Core auto-configuration (sync/async clients, pools, request config, retries) |
| `spring-boot-starter-httpclient5-actuator` | Micrometer/Actuator metrics |
| `spring-boot-starter-httpclient5-resilience4j` | Resilience4j circuit breaker / rate limiter integration |
| `spring-boot-starter-httpclient5-web` | Bidirectional HTTP header propagation and RestTemplate integration |
| `spring-boot-starter-httpclient5-all` | POM aggregator for all starters |
| `spring-boot-starter-httpclient5-integration-tests` | End-to-end tests and sample application (published) |

## Toolchain

- Target: **Java 17** (`java.version=17`, `maven.compiler.release=17`).
- Build JDK: **17 minimum, validated with JDK 25**. The build must stay JDK 25 compatible.
- Maven: **3.9.11** through the wrapper (`mvnw`, `mvnw.cmd`).
- Spring Boot: **4.0.4** (parent BOM). HttpClient 5 version is inherited from the BOM (5.5.x).
- Lombok and `spring-boot-configuration-processor` are annotation processors.

### JDK 23+ / JDK 25 specifics

1. `maven-compiler-plugin` is configured with `<proc>full</proc>` in the root POM. Since JDK 23,
   javac no longer discovers annotation processors from the classpath by default; without this
   flag Lombok silently generates nothing and compilation fails with hundreds of `cannot find symbol`.
2. `.mvn/jvm.config` contains the required `--add-opens` flags. It is picked up by every Maven
   invocation, including the forked Maven started by `release:perform`. **Do not export
   `JDK_JAVA_OPTIONS` before a release anymore.**
3. Do not use Java 18+ APIs in tests/main code: tests are compiled with `--release 17`
   (example: `Thread.isVirtual()` must be called through reflection).

## Key commands (PowerShell)

```powershell
# Full build + tests (requires Docker for Testcontainers)
.\mvnw.cmd clean verify

# Fast compile
.\mvnw.cmd clean install -DskipTests

# One module
.\mvnw.cmd test -pl spring-boot-starter-httpclient5
.\mvnw.cmd test -pl spring-boot-starter-httpclient5-web

# Compile only (no container), useful after POM changes
.\mvnw.cmd clean test-compile
```

Quote Maven properties containing dots in PowerShell, e.g. `"-DskipTests"`, `"-Dgpg.skip=true"`.
Otherwise PowerShell splits the argument (`Unknown lifecycle phase ".skip=true"`).

Notes:

- `clean` may fail on `target/checkout` left by a previous `release:perform` (read-only Git objects).
  Remove it with `cmd /c "rmdir /s /q target"` before rebuilding.
- The `settings.xml` warning `Unrecognised tag: 'repositories'` comes from the local machine
  configuration, not from this project.

## Tests

- Unit tests live in `src/test/java` next to the code they cover.
- Integration tests that need an HTTP server use a **Testcontainers `httpbin` container**
  (`mccutchen/go-httpbin`), started by the `HttpBinTestSupport` class of each module and exposed
  through `AbstractHttpBinTest` (`@DynamicPropertySource` sets `app.httpbin-host`).
- **Docker must be running** for `verify`. No test may depend on an external/private host
  (the old `nas.capsi-informatique.fr` endpoint must never come back).
- Unreachable endpoints are simulated with `http://127.0.0.1:1`.
- Useful coverage added: converters, `PatternUtils`, credentials parsing, `RequestConfigProperties`
  merge/copy, retry strategy, route planner, header propagation, `HeadersFilter`, zero-conf defaults.
- `HttpClient5ConfigIntegrationTest` is disabled on purpose (kept as documentation).

## Release

The release is a **single command**, run from the matching release branch (`hc5-sb42`,
`hc5-sb41`, `hc5-sb4` or `hc5-sb3`) — never from `develop`:

```powershell
.\mvnw.cmd release:prepare release:perform
```

Before releasing `hc5-sb42`, merge the validated `develop` state into it (merge request or
`git merge --no-ff develop`). For the maintenance lines, cherry-pick the fixes that must ship.
After the release, merge the release commits back into `develop`.

### One-time setup

1. Install a GPG key and publish it to a keyserver:
   `gpg --list-secret-keys`, `gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>`
2. Store the passphrase once in a **permanent Windows user variable** (restart the terminal after):
   ```powershell
   setx MAVEN_GPG_PASSPHRASE "your-passphrase"
   ```
   `maven-gpg-plugin` 3.2.8 reads `MAVEN_GPG_PASSPHRASE` by default (recommended practice).
3. Make sure the branch tracks `origin` (the release plugin pushes commits and tags):
   `git push -u origin <branch>`. The POM uses the **HTTPS** SCM URL
   (`developerConnection`) so the push reuses the Git credential helper; if you prefer SSH,
   override `developerConnection` or authenticate `git@github.com`.
4. Maven Central credentials: a server with id `central` in `~/.m2/settings.xml`.
5. Docker running (the release runs `clean verify`).

### What the POM does

- `maven-release-plugin`:
  - `releaseProfiles=release-sign-artifacts` → no need for `-Prelease-sign-artifacts`.
  - `pushChanges=true` → commits, tags and pushes automatically.
  - `localCheckout=true` + `preparationGoals=clean verify`.
  - Tag format: `<artifactId>-<version>` (e.g. `spring-boot-starter-httpclient5-parent-2.0.2`).
- `maven-gpg-plugin` is only active in the `release-sign-artifacts` profile (perform), so normal
  builds never sign.
- `central-publishing-maven-plugin` publishes automatically (`autoPublish=true`,
  `waitUntil=published`).
- `.mvn/jvm.config` removes the need for any `JDK_JAVA_OPTIONS` export.

### Recovery

- Failure during `release:prepare`: fix the cause, then `.\mvnw.cmd release:rollback`, delete
  leftover tags/commits if needed.
- Failure during `release:perform`: the release commit and tag are already pushed. Re-run
  `.\mvnw.cmd release:perform` from the checkout/tag, or use
  `.\mvnw.cmd release:perform -Drelease.properties` / the generated `release.properties`.
- Never commit `release.properties` or `pom.xml.releaseBackup` / `pom.xml.tag` / `pom.xml.next`.

## Architecture notes

- Configuration prefix: `spring.httpclient5`.
- `HttpClient5Config` holds nested configuration models and compiles request rules
  (`method + URI` regex patterns) into a LRU cache.
- Host-specific pool configuration keys are normalized (`http` → port 80, `https` → port 443).
- Request configurations are resolved by pattern matching, then merged from least to most
  specific (`RequestConfigProperties.apply`).
- The adaptive response timeout policy (actuator starter, 2.2.x only) is a core SPI
  (`AdaptiveTimeoutPolicy`) applied by `HttpClient5Config` after the request configuration
  resolution. The actuator module supplies `MicrometerAdaptiveTimeoutPolicy` and
  `AdaptiveTimeoutExecChainHandler`. The policy is global; a request rule can only toggle it with
  `adaptive-timeout.enabled`.
- Async and sync pools are independent (`pool` / `async-pool`).
- Header propagation is wired by the web starter through request-scoped storages
  (`hc5DownHeaders` / `hc5UpHeaders`).
- `AsyncExecutorConfig` (web starter) registers a `TaskDecorator` bean to propagate the request
  context (and therefore request-scoped header storages) to `@Async` tasks. It must stay a
  `TaskDecorator` bean, not only a `ThreadPoolTaskExecutorCustomizer`: with
  `spring.threads.virtual.enabled=true`, Boot uses a `SimpleAsyncTaskExecutor` and ignores the
  thread-pool customizer.

## Development workflow

### Branch flow

1. Implement and test every change on `develop` first; normal merge requests target `develop`.
2. Keep one logical commit per change so it can be cherry-picked cleanly.
3. Promote to `hc5-sb42` (same Spring Boot line) with a merge request from `develop`, or with a
   fast-forward/merge when the branch has not diverged.
4. Backport to `hc5-sb41`, `hc5-sb4` and `hc5-sb3` from a dedicated branch with
   `git cherry-pick -x <commit>`, then open a merge request. Adapt the code only for the
   Boot/HttpClient API differences; do not bump the libraries of older branches.
5. Release only from the `hc5-sb*` branches. `develop` keeps a `-SNAPSHOT` version and is never
   released.
6. After a release, merge the release commits (`[maven-release-plugin] prepare release` and the
   next `-SNAPSHOT` bump) back into `develop`, so both branches share the same version.

### Commands

```powershell
# 1. Work on develop
git switch develop
# ... edit, test ...
git commit -m "feat: ..."
git push origin develop

# 2. Promote to the 4.2 release branch (prefer a merge request)
git switch hc5-sb42
git merge --no-ff develop
git push origin hc5-sb42

# 3. Backport one commit to a maintenance line (prefer a merge request)
git switch -c backport/my-fix hc5-sb41
git cherry-pick -x <commit>
.\mvnw.cmd test -pl <affected-module>
git push -u origin backport/my-fix

# 4. Sync develop after a release
git switch develop
git merge --no-ff hc5-sb42
```

Cherry-pick rules:

- Always use `-x`; the original commit is then recorded in the message.
- Never cherry-pick a `[maven-release-plugin]` commit to another line.
- POM version and API conflicts are expected: resolve them with the **target** branch's versions.
- A change that cannot compile on an older line stays specific to that line and must say so in
  its commit message.

### Implementation rules

1. Change the core module first.
2. Add/extend unit tests; prefer plain unit tests over Spring contexts for configuration logic.
3. Run `.\mvnw.cmd test -pl <module>` then `.\mvnw.cmd clean verify`.
4. Keep the POM dependency versions centralized in the root `pom.xml`.
5. Do not reintroduce external test endpoints.
6. When a configuration property or behaviour changes, update `docs/configuration-guide.md`,
   `docs/CONFIGURATION-ALL.md` and, if relevant, `README.md`. Documentation must match the code,
   not the old AI-generated examples.
7. When an actuator meter, tag or observation changes, update `docs/actuator-metrics.md`.
