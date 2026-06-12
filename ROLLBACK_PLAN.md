# Plan de Rollback - CircleGuard

Este documento describe el procedimiento a seguir si un despliegue a
producción (namespace `master`) presenta fallas tras la aprobación manual
del pipeline `Jenkinsfile.master`.

## 1. Rollback de los microservicios (Kubernetes)

Cada Deployment generado por `k8s/templates/*.yml` mantiene historial de
revisiones gracias al `RollingUpdate` por defecto de Kubernetes.

```bash
# Ver historial de revisiones de un servicio
kubectl rollout history deployment/<servicio>-deployment -n master

# Volver a la revisión anterior
kubectl rollout undo deployment/<servicio>-deployment -n master

# Volver a una revisión específica
kubectl rollout undo deployment/<servicio>-deployment -n master --to-revision=<N>

# Verificar el estado tras el rollback
kubectl rollout status deployment/<servicio>-deployment -n master
```

Repetir para cada uno de los 6 microservicios afectados:
`gateway-service`, `auth-service`, `identity-service`, `form-service`,
`notification-service`, `promotion-service`.

## 2. Rollback de la imagen Docker

Si el rollback de Kubernetes no es suficiente (por ejemplo, la imagen
anterior ya no está en el historial de revisiones), redeployar
explícitamente la versión anterior:

```bash
kubectl set image deployment/<servicio>-deployment \
  <servicio>=circleguardacr.azurecr.io/circleguard-<servicio>:<IMAGE_TAG_anterior> \
  -n master
```

El `IMAGE_TAG_anterior` corresponde al `BUILD_NUMBER` del build estable
previo (visible en Jenkins y en el `RELEASE_NOTES.md` de ese release).

## 3. Rollback del código fuente

```bash
# Identificar el último tag estable
git tag --sort=-creatordate | head -5

# Revertir los commits problemáticos sin perder historial
git revert <commit-problema>

# Crear un nuevo tag de release correctivo
git tag -a v1.0.<N+1> -m "Rollback: revertir cambios de v1.0.<N>"
git push origin <rama> --tags
```

No se utiliza `git reset --hard` ni reescritura de historial para evitar
romper el historial compartido en `master`.

## 4. Rollback de infraestructura base (Postgres, RBAC, etc.)

Los manifiestos en `k8s/infrastructure/` son declarativos. Para volver a un
estado anterior:

```bash
# Re-aplicar la versión anterior del manifiesto desde el tag estable
git checkout <tag-estable> -- k8s/infrastructure/
envsubst < k8s/infrastructure/<archivo>.yml | kubectl apply -f -
```

## 5. Criterios para decidir un rollback

- Falla el `rollout status` de uno o más microservicios durante el deploy.
- Errores 5xx sostenidos en el Gateway tras el despliegue.
- Falla de conexión a la base de datos (Postgres/Neo4j) por cambios de
  configuración o RBAC.

## 6. Responsables

El rollback debe ser ejecutado por quien aprobó el despliegue a producción
en el stage **"Approval for Production Deploy"** del pipeline
`Jenkinsfile.master`, o por cualquier integrante del equipo con acceso al
kubeconfig de `circleguard-aks`.
