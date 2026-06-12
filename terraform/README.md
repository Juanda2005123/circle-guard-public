# Terraform — CircleGuard

Este directorio tiene dos módulos independientes:

## `azure-infra/`
Provisiona la infraestructura cloud: cluster AKS (`circleguard-aks`) y el
registry ACR (`circleguardacr`). Backend remoto en Azure Storage
(`circleguardtfstate`). Ver `azure-infra/`.

## Raíz (`main.tf`, `modules/security`)
Gestiona el `Secret` de Kubernetes `circleguard-secrets` (credenciales de
Postgres, Neo4j y LDAP) en cada namespace del cluster AKS, usando el
provider `kubernetes` apuntando al context `circleguard-aks`.

Cada ambiente es un **Terraform workspace** independiente, para que el
estado de `dev`/`stage`/`prod` no se pisen entre sí:

| Ambiente Jenkins | Namespace k8s | Workspace Terraform | tfvars                |
|-------------------|----------------|----------------------|-----------------------|
| dev                | `dev`          | `dev`                | `environments/dev.tfvars`   |
| stage              | `stage`        | `stage`              | `environments/stage.tfvars` |
| master             | `master`       | `prod`               | `environments/prod.tfvars`  |

### Aplicar cambios (manual)

Las credenciales reales van en `terraform/environments/app.secrets.tfvars`
(NO se commitea — está en `.gitignore` por el patrón `*.secrets.tfvars`).
Cada desarrollador debe crear su propia copia localmente:

```bash
cd terraform
terraform workspace select dev   # o stage / prod
terraform apply \
  -var-file=environments/dev.tfvars \
  -var-file=environments/app.secrets.tfvars
```

Los módulos `modules/base-infrastructure` y `modules/applications` quedaron
sin usar: esos recursos (Postgres, Redis, Neo4j, Kafka, microservicios, etc.)
ya se despliegan vía Jenkins (`kubectl apply` + `envsubst` sobre
`k8s/infrastructure/` y `k8s/templates/`). Mantenerlos activos duplicaría y
entraría en conflicto con esos recursos.
