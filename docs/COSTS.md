# Costos de Infraestructura — CircleGuard (Azure)

> Estimaciones basadas en precios públicos de Azure para la región
> `eastus2` (pay-as-you-go, USD), a fecha de esta entrega. Son valores
> aproximados — para un costo exacto usar la **Azure Pricing Calculator**
> con la suscripción real del proyecto.

## 1. Recursos de Azure utilizados

Según `terraform/azure-infra/` y `terraform/main.tf`:

| Recurso | SKU / Configuración | Uso |
|---|---|---|
| AKS — `circleguard-aks` | 2x nodos `Standard_B2s` (2 vCPU, 4 GiB RAM, burstable) | Cómputo para los namespaces `dev`, `stage`, `master` |
| AKS Control Plane | Tier **Free** (asumido) | Plano de control del cluster |
| ACR — `circleguardacr` | Tier **Basic** (10 GiB de almacenamiento incluido) | Registro de las 6 imágenes Docker x 3 ambientes |
| Storage Account — `circleguardtfstate` | Standard LRS, Blob | Backend remoto del estado de Terraform (`azure-infra/`) |
| Resource Group | `circleguard-rg` | Agrupación lógica, sin costo propio |
| Load Balancer / NodePort | Exposición del Gateway (puerto `30087`) | Si se usa `Service type: LoadBalancer`, agrega costo de LB; con `NodePort` (actual) no hay costo adicional |

## 2. Estimación mensual (24/7)

| Recurso | Costo unitario aprox. | Cantidad | Subtotal mensual |
|---|---|---|---|
| `Standard_B2s` (~US$0.0832/hora) | ~US$60.7/mes c/u | 2 nodos | **~US$121.5** |
| AKS Control Plane (Free tier) | US$0 | 1 | **US$0** |
| ACR Basic | ~US$5.0/mes | 1 | **~US$5.0** |
| Storage Account (tfstate, pocos MB) | <US$0.10/mes | 1 | **~US$0.1** |
| Transferencia de datos saliente (estimado, uso bajo) | — | — | **~US$1–5** |
| **Total estimado** | | | **≈ US$128 – 132 / mes** |

> Este cluster de 2 nodos **es compartido** por los 3 namespaces
> (`dev`/`stage`/`master`) — el costo de cómputo **no se multiplica** por
> ambiente, ya que todos corren sobre la misma infraestructura física.

### Optimización (FinOps) — no implementado, recomendaciones
- Usar **Spot VMs** para el node pool de `dev`/`stage` (ahorro ~60-80% en `Standard_B2s`, ~US$25-50/mes adicionales de ahorro), reservando nodos on-demand solo para `master`.
- **Auto-scaling** del node pool (`az aks nodepool` con `--enable-cluster-autoscaler`) para escalar a 1 nodo en horarios de baja carga.
- **Apagar el cluster fuera de horario de clases** (AKS permite `az aks stop`/`az aks start`), relevante porque el SLA de uptime objetivo es 7am-10pm.

## 3. Capacidad estimada de usuarios

### Recursos disponibles vs. solicitados

Cada nodo `Standard_B2s` aporta 2 vCPU / 4 GiB RAM; descontando overhead de
sistema (kubelet, daemonsets de AKS, ~0.5-0.7 vCPU y ~0.8-1 GiB por nodo),
quedan aprox. **~2.6 vCPU / ~6.4 GiB RAM utilizables** en los 2 nodos.

Recursos `requests` actuales por namespace (`k8s/templates/` + `k8s/infrastructure/`):

| Componente | CPU request | Mem request |
|---|---|---|
| 6 microservicios (gateway, auth, identity, form, notification, promotion) — 100m / 200Mi c/u | 600m | 1.2 GiB |
| Kafka + Zookeeper | ~350m | ~1.1 GiB |
| Neo4j | 250m | 1 GiB |
| OpenLDAP | 100m | 64 MiB |
| PostgreSQL, Redis (estimado) | ~250m | ~512 MiB |
| **Total por namespace** | **~1.5 vCPU** | **~3.9 GiB** |

Con esto, **un solo namespace cabe holgadamente** en los 2 nodos, pero los
**3 namespaces simultáneos** (`dev`+`stage`+`master`, ~4.5 vCPU / ~11.7 GiB
solicitados) **exceden la capacidad** de 2x `Standard_B2s` — en la práctica
esto es aceptable porque `dev`/`stage` no necesitan estar siempre activos a
plena carga, pero es una limitante real si los 3 ambientes deben correr
producción simultáneamente con holgura.

### Estimación de usuarios concurrentes (namespace `master`, configuración actual)

Con **1 réplica por microservicio** y `limits` de 500m CPU / 512Mi por pod
(sin Horizontal Pod Autoscaler), cada microservicio Java/Spring Boot puede
sostener aproximadamente **20-50 requests/segundo** en operaciones
livianas (lecturas simples, checks de estado) antes de saturar su CPU
limit, y considerablemente menos (~5-15 req/s) en operaciones que golpean
Postgres/Neo4j (formularios, promoción de estado).

Asumiendo un patrón de uso típico de check-in universitario (ráfagas
cortas, no tráfico sostenido constante — un estudiante hace 2-3 llamadas
en el momento del check-in), una estimación conservadora es:

| Escenario | Estimación |
|---|---|
| Usuarios concurrentes activos (haciendo requests en el mismo segundo) | **~30-50** |
| Usuarios totales soportados en una ventana de check-in de clase (~10 min) | **~500-800** |
| Throughput sostenido del Gateway antes de degradación notable | **~30-40 req/s** |

> Para validar estos números con datos reales, ejecutar
> `performance-tests/locustfile.py` (ver [HOW_TO_RUN.md](../HOW_TO_RUN.md))
> contra el namespace `master` y registrar el punto de quiebre (latencia
> p95 > 1s o tasa de error > 1%).

### Cómo escalar si se supera esta capacidad
1. **Horizontal Pod Autoscaler (HPA)** para `gateway-service` y
   `promotion-service` (los más expuestos al tráfico de entrada y a
   operaciones de grafo).
2. Subir el SKU de los nodos (`Standard_B2s` → `Standard_B4ms`, 4 vCPU/16GB)
   o agregar un tercer nodo al pool.
3. Separar Postgres/Neo4j/Kafka a un node pool dedicado para no competir
   por CPU con los microservicios.
