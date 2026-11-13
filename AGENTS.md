# AGENTS.md - spring-boot-httpclient5

## Project Structure Overview

This is a Spring Boot starter project for Apache HttpClient 5 with multiple submodules:
- `spring-boot-starter-httpclient5` - Core HTTP client functionality
- `spring-boot-starter-httpclient5-web` - Web integration with header propagation
- `spring-boot-starter-httpclient5-resilience4j` - Resilience4j circuit breaker integration
- `spring-boot-starter-httpclient5-actuator` - Actuator monitoring integration

## Key Commands

### Build the project:
```bash
./mvnw clean install
```

### Run tests for a specific module:
```bash
./mvnw test -pl spring-boot-starter-httpclient5
```

### Run integration tests:
```bash
./mvnw verify -DskipTests=false
```

## Configuration Files

Configuration is documented in:
- `docs/configuration-guide.md` - Main configuration guide
- `docs/CONFIGURATION-ALL.md` - Complete parameter reference
- `docs/usage-example.md` - Usage examples

## Testing Approach

- Unit tests are in `src/test/java`
- Integration tests use embedded servers and real HTTP endpoints
- Tests verify configuration patterns, connection pooling, and request handling
- Test coverage includes all submodules

## Important Notes

1. Configuration is applied using pattern matching based on method + URI
2. Host-specific configurations take precedence over default ones
3. The `spring-boot-starter-httpclient5-resilience4j` requires Resilience4j configuration in standard Spring Boot format
4. The web starter provides header propagation capabilities that can be configured via request patterns

## Development Workflow

1. Make changes to the core module first
2. Test with unit tests before integration testing
3. Verify configuration works across all submodules
4. Run full test suite after major changes