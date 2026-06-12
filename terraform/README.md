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

## Backend remoto del estado

Ambos módulos usan **backend remoto** (ningún `.tfstate` se guarda en local
ni en git):

| Módulo | Backend | Ubicación del estado |
|---|---|---|
| `azure-infra/` | `azurerm` | Storage Account `circleguardtfstate`, contenedor `tfstate`, key `azure-infra.tfstate` |
| Raíz (`main.tf`) | `kubernetes` | `Secret` de Kubernetes con sufijo `circleguard-state`, en el cluster `circleguard-aks` (un Secret distinto por **workspace**: `dev`/`stage`/`prod`) |

El backend `kubernetes` del módulo raíz es intencional: ese módulo ya
gestiona recursos **dentro** del propio cluster AKS (los `Secret`
`circleguard-secrets`), así que guardar su estado como otro `Secret` en ese
mismo cluster evita depender de credenciales adicionales de Azure Storage
solo para el estado, mientras sigue siendo remoto y compartido por el
equipo (no local). `azure-infra/` usa `azurerm` porque provisiona el propio
cluster — su estado no puede vivir dentro de un recurso que aún no existe.

## Diagrama de infraestructura

```
┌────────────────────────────────────────────────────────────────────┐
│ Azure Subscription                                                   │
│                                                                       │
│  Resource Group: circleguard-rg                                      │
│  ┌─────────────────────────────┐   ┌──────────────────────────────┐│
│  │ AKS: circleguard-aks         │   │ Storage Account:              ││
│  │ (2x Standard_B2s, eastus2)   │   │ circleguardtfstate             ││
│  │                               │   │  └─ container tfstate         ││
│  │  namespaces: dev/stage/master│   │      └─ azure-infra.tfstate    ││
│  │   └─ Secret                  │   └──────────────────────────────┘│
│  │      circleguard-secrets     │              ▲                     │
│  │      (Postgres/Neo4j/LDAP)   │              │ state               │
│  │   └─ Secret                  │   ┌──────────┴──────────────────┐ │
│  │      circleguard-state       │   │ terraform/azure-infra/        │ │
│  │      (estado del módulo raíz,│   │  module "aks" -> AKS + ACR    │ │
│  │       por workspace)         │◀──┤  (backend: azurerm)            │ │
│  │            ▲                 │   └────────────────────────────────┘
│  │            │ state           │
│  │  ┌─────────┴───────────────┐ │
│  │  │ terraform/main.tf         │ │
│  │  │  module "security"        │ │
│  │  │  (workspaces dev/stage/   │ │
│  │  │   prod, backend: k8s)     │ │
│  │  └───────────────────────────┘
│  │                               │
│  │  ┌──────────────────────────┐│
│  │  │ ACR: circleguardacr        ││
│  │  └──────────────────────────┘│
│  └─────────────────────────────┘
└────────────────────────────────────────────────────────────────────┘
```

> El resto de la infraestructura del clúster (Postgres, Neo4j, Redis, Kafka,
> OpenLDAP, RBAC y los 6 microservicios) **no** es gestionada por Terraform,
> sino por Jenkins vía `kubectl apply` + `envsubst` sobre
> `k8s/infrastructure/` y `k8s/templates/` — ver [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md).

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
