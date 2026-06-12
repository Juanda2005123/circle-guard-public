# Design Patterns in CircleGuard

This document identifies the design patterns used across the CircleGuard microservices architecture: patterns already present in the existing codebase, plus the new patterns added as part of HU5 (Resilience and Configuration).

## Existing Patterns

### 1. Repository Pattern
- **Where**: `services/circleguard-auth-service/.../repository/LocalUserRepository.java`, `services/circleguard-promotion-service/.../repository/jpa/BuildingRepository.java`, `services/circleguard-promotion-service/.../repository/graph/CircleNodeRepository.java`
- **What**: Spring Data JPA and Spring Data Neo4j repositories abstract persistence behind interfaces with declarative query methods.
- **Benefit**: Business logic stays decoupled from the underlying storage technology (relational vs. graph database), and repositories are easy to mock in tests.

### 2. DTO (Data Transfer Object) Pattern
- **Where**: `services/circleguard-promotion-service/.../dto/FloorDTO.java`, `AccessPointDTO.java`, `BuildingDTO.java`
- **What**: Dedicated `@Builder`/`@Data` classes shape the data exchanged over REST APIs, separate from JPA/Neo4j entities.
- **Benefit**: Internal domain models can evolve without breaking API contracts, and avoids leaking persistence annotations/relationships to clients.

### 3. Anti-Corruption Layer / Service Client Adapter
- **Where**: `services/circleguard-auth-service/.../client/IdentityClient.java`
- **What**: A dedicated client component wraps HTTP calls from auth-service to identity-service (e.g., to resolve anonymous IDs).
- **Benefit**: Isolates inter-service communication details in one place, so the calling service's domain logic doesn't depend on the other service's API shape, and the transport mechanism (RestTemplate, Feign, WebClient) can change transparently.

### 4. Strategy / Chain of Responsibility (Dual-Chain Authentication)
- **Where**: `services/circleguard-auth-service/.../security/DualChainAuthenticationProvider.java`
- **What**: Multiple `AuthenticationProvider` strategies (LDAP, then local DB) are chained; if one fails, the next is tried.
- **Benefit**: New authentication sources can be added or reordered without changing the core authentication flow.

### 5. API Gateway Pattern
- **Where**: `services/circleguard-gateway-service` (Spring Cloud Gateway MVC)
- **What**: A single entry point routes requests to the 5 backend microservices based on path predicates (`/api/v1/auth/**`, `/api/v1/identities/**`, etc.).
- **Benefit**: Centralizes cross-cutting concerns (routing, JWT validation, rate-limiting potential, circuit breaking) instead of duplicating them in every service.

### 6. Dependency Injection (Constructor Injection)
- **Where**: Ubiquitous via Lombok `@RequiredArgsConstructor` (e.g., `LoginController`, `BuildingService`).
- **What**: Dependencies are declared as `final` fields and injected via constructor, rather than field injection.
- **Benefit**: Makes dependencies explicit, enables immutability, and simplifies unit testing with mocks.

---

## New Patterns Added (HU5)

### 7. Circuit Breaker (Resilience Pattern)
- **Where**: `services/circleguard-gateway-service/src/main/resources/application.yml` (route filter `CircuitBreaker` on the `auth-service` route) + `FallbackController.java`
- **What**: Resilience4j wraps calls from the gateway to auth-service. After 5+ calls with a ≥50% failure rate (within a sliding window of 10), the circuit opens for 10s and requests are short-circuited to a fallback response (`503` via `/fallback/auth`) instead of failing slowly or with raw 500/timeout errors.
- **Benefit**: Prevents cascading failures and gives the downstream service time to recover; the gateway degrades gracefully (clear `503` message) instead of hanging or returning opaque errors.
- **Demonstrated**: stop `auth-service` → gateway returns `503` fallback; restart it → circuit auto-recovers to `200` after the wait window, with no manual intervention.

### 8. External Configuration / Feature Toggle (Configuration Pattern)
- **Where**: `services/circleguard-auth-service/.../config/FeatureFlagsProperties.java` (`@ConfigurationProperties(prefix = "app.features")`) + `LoginController.generateVisitorHandoff()`
- **What**: The visitor handoff endpoint checks `app.features.visitor-handoff-enabled` (default `true`, overridable via `application.yml` or the `APP_FEATURES_VISITORHANDOFFENABLED` environment variable) and returns `404` when disabled.
- **Benefit**: Features can be turned on/off per environment (dev/stage/prod) or as an emergency kill-switch, without recompiling or redeploying code.
