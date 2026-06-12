# Patrones de Diseño — CircleGuard

## 1. Patrones ya presentes en la arquitectura

| Patrón | Dónde | Propósito |
|---|---|---|
| **API Gateway** | `gateway-service` (Spring Cloud Gateway) | Punto único de entrada que rutea por `Path` hacia `auth-service`, `identity-service`, `form-service`, `promotion-service`, ocultando la topología interna al cliente. |
| **Repository** | `LocalUserRepository` (auth-service) y equivalentes en cada servicio | Encapsula el acceso a datos (PostgreSQL) detrás de una interfaz, separando lógica de negocio de persistencia. |
| **Event-Driven / Observer** | `@KafkaListener` — `SurveyListener` (promotion-service), `ExposureNotificationListener`, `PriorityAlertListener`, `CircleFencedListener` (notification-service) | Los servicios reaccionan a eventos publicados en Kafka (cambios de estado, encuestas, exposición) sin acoplarse directamente al productor. |
| **Anonymization Vault** *(patrón de seguridad propio del dominio)* | `identity-service` + PostgreSQL | Segrega el mapeo identidad-real ↔ identidad-anónima del resto del grafo de contactos (Neo4j), cumpliendo FERPA. |

## 2. Patrones implementados/mejorados en este entregable

### 2.1 Circuit Breaker (Resiliencia)

**Dónde:** `gateway-service` → `auth-service`, vía Resilience4j.

**Configuración** (`services/circleguard-gateway-service/src/main/resources/application.yml`):
```yaml
spring:
  cloud:
    gateway:
      mvc:
        routes:
          - id: auth-service
            uri: http://auth-service:8180
            predicates:
              - Path=/api/v1/auth/**
            filters:
              - name: CircuitBreaker
                args:
                  id: authServiceCB
                  fallbackPath: /fallback/auth

resilience4j:
  circuitbreaker:
    instances:
      authServiceCB:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
  timelimiter:
    instances:
      authServiceCB:
        timeout-duration: 3s
```

**Fallback** (`FallbackController.java`):
```java
@RequestMapping("/fallback/auth")
public ResponseEntity<Map<String, String>> authFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "message", "Auth service is currently unavailable. Please try again later."
    ));
}
```

**Funcionamiento:** si de las últimas 10 llamadas a `auth-service` (mínimo 5
para evaluar) más del 50% fallan o tardan más de 3s, el circuito pasa a
`OPEN` durante 10s y el gateway responde de inmediato con `503` desde
`/fallback/auth`, sin propagar la espera/error hacia el cliente. Tras 10s
pasa a `HALF-OPEN` y permite 3 llamadas de prueba para decidir si vuelve a
`CLOSED`.

**Beneficio:** evita que la caída de `auth-service` agote hilos/conexiones
del gateway y produzca timeouts en cascada hacia el resto del sistema.

**Evidencia:** [Circuit_breaker.png](../Circuit_breaker.png), [auth_curl.png](../auth_curl.png), [auth_grafana.png](../auth_grafana.png).

---

### 2.2 External Configuration (Configuración)

**Dónde:** todos los Deployments en `k8s/templates/*.yml`.

Las imágenes Docker de los 6 microservicios son **idénticas** entre `dev`,
`stage` y `master`; lo que cambia por ambiente (URLs de bases de datos,
host/puerto de Redis, namespace, tag de imagen) se inyecta en tiempo de
despliegue como variables de entorno, vía manifiestos templados con
`envsubst`:

```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:postgresql://postgres:5432/..."
  - name: SPRING_DATA_REDIS_HOST
    value: "redis"
  - name: SPRING_DATA_REDIS_PORT
    value: "6379"
```

Las credenciales sensibles (Postgres, Neo4j, LDAP) van un paso más allá: se
gestionan como `Secret` de Kubernetes (`circleguard-secrets`), provisionado
por Terraform (`terraform/modules/security`) y consumido vía
`secretKeyRef` — nunca hardcodeadas en la imagen ni en el manifiesto.

**Beneficio:** un mismo artefacto (imagen Docker) se promueve sin
recompilar entre dev → stage → prod; el comportamiento por ambiente se
controla externamente (manifiesto + Secret), reduciendo el riesgo de
"funciona en mi build" y permitiendo rotar credenciales sin tocar código.

---

### 2.3 Feature Toggle (Configuración)

**Dónde:** `auth-service`.

```java
// FeatureFlagsProperties.java
@Configuration
@ConfigurationProperties(prefix = "app.features")
public class FeatureFlagsProperties {
    private boolean visitorHandoffEnabled = true;
    // getters/setters
}
```

```java
// LoginController.java
@PostMapping("/visitor/handoff")
public ResponseEntity<Map<String, String>> generateVisitorHandoff(...) {
    if (!featureFlags.isVisitorHandoffEnabled()) {
        return ResponseEntity.notFound().build();
    }
    ...
}
```

El flag `app.features.visitor-handoff-enabled` controla si el endpoint de
*handoff* de visitantes está activo, sin necesidad de desplegar una versión
distinta del servicio — basta con cambiar la propiedad (vía `application.yml`
o variable de entorno `APP_FEATURES_VISITORHANDOFFENABLED`) y reiniciar el
pod.

**Beneficio:** permite activar/desactivar funcionalidades por ambiente
(p. ej. probar `visitor handoff` solo en `dev`/`stage` antes de habilitarlo
en `master`) sin ramas de código separadas ni rebuilds.

## 3. Resumen

| Categoría requerida | Patrón | Estado |
|---|---|---|
| Resiliencia | Circuit Breaker (Resilience4j, gateway → auth) | ✅ Implementado y documentado |
| Configuración | External Configuration (env vars + Secrets vía K8s/Terraform) | ✅ Implementado y documentado |
| Configuración / adicional | Feature Toggle (`FeatureFlagsProperties`, auth-service) | ✅ Implementado y documentado |
