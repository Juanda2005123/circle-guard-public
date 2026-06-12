# Metodología Ágil — CircleGuard

## 1. Marco de trabajo

El equipo trabaja bajo **Scrum**, con el backlog y las historias de usuario
gestionadas en **Jira** (proyecto `IDSPF`). El trabajo de este entregable
corresponde a **IDSPF Sprint 2**, enfocado en estabilización de
infraestructura, calidad continua, observabilidad, resiliencia y gobernanza
del pipeline de producción.

## 2. Roles

| Rol | Responsable |
|---|---|
| Informador / Product Owner | Juan David Quintero |
| Arquitecto Cloud / DevOps | Juan David Quintero |
| SRE / Resiliencia | Juan Andrés |

## 3. Backlog — IDSPF Sprint 2

### Historia 1: Estabilización de Despliegues Locales y Secrets
**Como** Arquitecto Cloud, **quiero** automatizar la inyección de imágenes
Docker en el clúster Kind y securizar las contraseñas, **para que** los
despliegues en los pipelines no fallen por `ErrImagePull` y no existan
credenciales en texto plano.

**Criterios de aceptación:**
- El pipeline de Jenkins (dev y stage) carga correctamente las 6 imágenes al clúster.
- Postgres, Neo4j y OpenLDAP consumen credenciales desde un `Secret` de K8s, no variables de entorno en plano.
- Los tests E2E y de carga (Locust) ya no tienen `@Disabled` y corren en el pipeline de Stage.

**Estado:** Listo para Pruebas

---

### Historia 2: Implementación de Infraestructura como Código (Terraform)
**Como** DevOps Senior, **quiero** codificar la infraestructura base de
Kubernetes con Terraform, **para** poder provisionar y destruir los
ambientes (dev, stage, prod) de forma predecible y replicable.

**Criterios de aceptación:**
- Existe un directorio `terraform/` con estructura modular.
- `terraform apply` despliega correctamente namespaces y recursos base.
- Se usa un backend configurado para el manejo de estado.

**Estado:** Listo para Pruebas

---

### Historia 3: Integración de Calidad Continua (SonarQube y Trivy)
**Como** Tech Lead, **quiero** integrar herramientas de análisis estático y
de vulnerabilidades en Jenkins, **para** asegurar que el código no llegue a
producción con deuda técnica o fallos de seguridad en contenedores.

**Criterios de aceptación:**
- SonarQube integrado en el pipeline, genera reporte para los microservicios Java.
- Trivy escanea las imágenes Docker antes del despliegue y el pipeline falla con vulnerabilidades CRÍTICAS.
- Los pipelines requieren aprobación manual (`input`) antes de desplegar a producción.

**Estado:** Listo para Pruebas

---

### Historia 4: Stack de Observabilidad (Prometheus, Grafana, ELK)
**Como** Ingeniero de Confiabilidad (SRE), **quiero** desplegar un stack
completo de monitoreo y logging centralizado, **para** tener visibilidad en
tiempo real del estado de los microservicios y diagnosticar errores.

**Criterios de aceptación:**
- Prometheus y Grafana desplegados en el clúster.
- Existe al menos un Dashboard en Grafana con uso de CPU/RAM y estado de pods.
- Todos los microservicios tienen `readinessProbe` y `livenessProbe` apuntando a `/actuator/health`.

**Estado:** Listo para Pruebas
**Asignado:** Juan Andrés

---

### Historia 5: Experimentos de Caos y Resiliencia
**Como** Arquitecto de Software, **quiero** implementar Chaos Engineering
(Chaos Mesh/Litmus) y un patrón Circuit Breaker, **para** demostrar que el
sistema se recupera automáticamente de fallos inesperados en red o pods.

**Criterios de aceptación:**
- Patrón Circuit Breaker implementado (Resilience4j) en al menos un punto crítico (Gateway → Auth).
- Se documenta y graba un experimento de caos donde se elimina aleatoriamente un pod de base de datos y el sistema se recupera sin intervención humana.

**Estado:** Por hacer
**Asignado:** Juan Andrés

---

### Historia 6: Gobernanza y Gestión de Cambios
**Como** DevOps Senior, **quiero** implementar control de acceso básico
(RBAC) y un proceso formal de gestión de cambios en el pipeline de
producción, **para que** los despliegues a producción requieran aprobación,
queden trazados mediante tags/release notes, y exista un procedimiento
documentado de reversión ante fallos.

**Criterios de aceptación:**
- `Jenkinsfile.master` requiere aprobación manual (`input`) antes de desplegar a producción.
- Existe ServiceAccount, Role y RoleBinding (RBAC) aplicados al namespace de producción, usados por los 6 microservicios.
- Cada despliegue a producción genera un tag de versión (`vX.Y.Z`) y un `RELEASE_NOTES.md` commiteado.
- Existe un plan de rollback documentado (`ROLLBACK_PLAN.md`).

**Estado:** Listo para Pruebas
**Asignado:** Juan Andrés

## 4. Trazabilidad HU → Entregable

| Historia | Entregable / Evidencia |
|---|---|
| HU1 | `k8s/infrastructure/*.yml` (Secrets K8s), pipelines `dev`/`stage`, tests E2E/Locust habilitados |
| HU2 | `terraform/` (módulos, workspaces dev/stage/prod) |
| HU3 | SonarQube + Trivy en `Jenkinsfile.*`, gate `input` en `Jenkinsfile.master` |
| HU4 | `observability/` (Prometheus, Grafana, Alertmanager), probes `/actuator/health` — ver [ARCHITECTURE.md §9](ARCHITECTURE.md#9-observabilidad) |
| HU5 | Circuit Breaker Gateway→Auth — ver [ARCHITECTURE.md §8](ARCHITECTURE.md#8-resiliencia-circuit-breaker); experimento de caos pendiente |
| HU6 | `k8s/infrastructure/rbac.yml`, stage "Approval for Production Deploy" y "Generate Release Notes" en `Jenkinsfile.master`, [ROLLBACK_PLAN.md](../ROLLBACK_PLAN.md) |
