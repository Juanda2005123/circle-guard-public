# Manual de Operaciones — CircleGuard

Este documento cubre la operación del sistema una vez desplegado en **AKS**
(ambientes `dev`, `stage`, `master`). Para levantar el entorno local
(Jenkins, Kind, pruebas) ver [HOW_TO_RUN.md](../HOW_TO_RUN.md).

## 1. Ejecutar un despliegue

Cada ambiente se despliega mediante su pipeline de Jenkins correspondiente:

| Job | Jenkinsfile | Namespace AKS | Rama |
|---|---|---|---|
| `circleguard-dev` | `Jenkinsfile.dev` | `dev` | `develop` |
| `circleguard-stage` | `Jenkinsfile.stage` | `stage` | `stage` |
| `circleguard-master` | `Jenkinsfile.master` | `master` | `master`/`main` |

Pasos generales (ver evidencia en [Entregables/Jenkins/](../Entregables/Jenkins/)):
1. Lanzar el build del job correspondiente en `http://localhost:8080`.
2. El pipeline corre tests, SonarQube, build/push de las 6 imágenes a ACR, escaneo Trivy.
3. **Solo en `master`**: el pipeline se detiene en el stage **"Approval for Production Deploy"** esperando aprobación manual.
4. Tras aprobar, se aplican `k8s/infrastructure/*.yml` y `k8s/templates/*.yml` vía `envsubst | kubectl apply -f -`.
5. **Solo en `master`**: se genera/commitea `RELEASE_NOTES.md` y se crea el tag `vX.Y.<build>`.

## 2. Aprobar un despliegue a producción

En el stage "Approval for Production Deploy", Jenkins muestra un prompt:
> ¿Aprobar despliegue a producción del build #N (rama master)?

Solo continuar si: los tests pasaron, SonarQube no reporta issues bloqueantes
y Trivy no encontró vulnerabilidades CRÍTICAS en los stages previos.

## 3. Verificación post-despliegue

```bash
# Estado general del namespace
kubectl get all -n <dev|stage|master>

# Verificar que los 6 microservicios estén Ready
kubectl rollout status deployment/<servicio>-deployment -n <namespace>

# Verificar que los pods usan la ServiceAccount RBAC
kubectl get pods -n <namespace> -o jsonpath='{range .items[*]}{.metadata.name}{"  ->  "}{.spec.serviceAccountName}{"\n"}{end}'
```

## 4. Monitoreo (Observabilidad)

Stack desplegado desde `observability/` (Prometheus, Grafana, Alertmanager).

- **Grafana**: dashboard general de CPU/RAM y estado de pods — ver [grafana_dashboard.png](../grafana_dashboard.png).
- **Prometheus**: métricas de los 6 microservicios vía `/actuator/health` y endpoints `/actuator/prometheus` — ver [prometheus_dashboard.png](../prometheus_dashboard.png).
- **Alertmanager**: alertas activas cuando un servicio cae — ver [alertmanager_all_up.png](../alertmanager_all_up.png) / [alertmanager_all_down.png](../alertmanager_all_down.png).

Procedimiento ante una alerta:
1. Revisar Alertmanager para identificar el servicio/pod afectado.
2. Revisar Grafana/Prometheus para correlacionar con métricas (CPU, memoria, errores).
3. `kubectl logs deployment/<servicio>-deployment -n <namespace>` para revisar el error puntual.
4. Si el problema viene de un despliegue reciente, seguir el [Plan de Rollback](../ROLLBACK_PLAN.md).

## 5. Circuit Breaker (Gateway → Auth)

El `gateway-service` implementa Circuit Breaker (Resilience4j) hacia
`auth-service`. Si `auth-service` no responde, el circuito se abre y el
gateway devuelve una respuesta de fallback en lugar de timeouts en cascada.

Verificación manual:
```bash
# Escalar auth-service a 0 réplicas para simular caída
kubectl scale deployment/auth-service-deployment -n <namespace> --replicas=0

# Probar el endpoint vía gateway (debería responder con fallback, no timeout)
curl http://<gateway-url>/api/auth/...

# Restaurar
kubectl scale deployment/auth-service-deployment -n <namespace> --replicas=1
```
Evidencia: [Circuit_breaker.png](../Circuit_breaker.png), [auth_curl.png](../auth_curl.png), [auth_grafana.png](../auth_grafana.png).

## 6. Rollback

Ver procedimiento completo en [ROLLBACK_PLAN.md](../ROLLBACK_PLAN.md):
- `kubectl rollout undo` por microservicio.
- Redeploy de imagen anterior por tag.
- `git revert` + nuevo tag correctivo.
- Re-aplicación de manifiestos de `k8s/infrastructure/` desde un tag estable.

## 7. Gestión de secretos

Las credenciales de Postgres, Neo4j y OpenLDAP se gestionan como
`kubernetes_secret` vía Terraform (`terraform/modules/security`), por
workspace (`dev`/`stage`/`prod`). Para rotar un secreto:

```bash
cd terraform
terraform workspace select <dev|stage|prod>
terraform apply -var-file="environments/<dev|stage|prod>.tfvars"
```

Tras aplicar, reiniciar los pods que consumen el secreto para que tomen el
nuevo valor:
```bash
kubectl rollout restart deployment/<servicio>-deployment -n <namespace>
```

## 8. Control de acceso (RBAC)

Cada namespace cuenta con una ServiceAccount `circleguard-app` (definida en
`k8s/infrastructure/rbac.yml`) con permisos de solo lectura sobre
`configmaps`, `secrets`, `services`, `endpoints` y `pods` del propio
namespace. Los 6 microservicios la usan vía `serviceAccountName`. No otorgar
permisos adicionales sin actualizar el `Role` correspondiente.
