# 🚀 Guía de Ejecución — Ecosistema DevOps & Testing (Taller 2)

Esta guía documenta los pasos y comandos necesarios para interactuar con el entorno de automatización local, el clúster de Kubernetes, y ejecutar las suites de pruebas automatizadas del ecosistema **CircleGuard**.

---

## 🛠️ 1. Levantar el Entorno de Herramientas (Jenkins & Infra)

Para levantar el servidor de automatización local (Jenkins), se dispone de un archivo de Docker Compose diseñado específicamente para las herramientas de CI/CD.

*   **Comando de ejecución:**
    Ejecuta el siguiente comando desde la raíz del proyecto para iniciar Jenkins en segundo plano:
    ```bash
    docker compose -f docker-compose.tools.yml up -d
    ```

*   **Acceso a la Interfaz Web:**
    Una vez inicializado el contenedor, la interfaz gráfica de Jenkins estará disponible en:
    ```
    http://localhost:8080
    ```

*   **Detalle de Configuración:**
    El contenedor `circleguard-jenkins` se ejecuta como usuario `root` para poder interactuar directamente con el socket de Docker del host (`/var/run/docker.sock`) y mapear de forma segura la configuración local de Kubernetes (directorio `.kube` del host hacia `/tmp/kube_host` dentro del contenedor), permitiendo que los pipelines se comuniquen con el clúster de Kind.

---

## ☸️ 2. Control del Clúster Kubernetes Local (Kind)

El despliegue de la infraestructura y servicios del proyecto se realiza sobre un clúster local de Kubernetes (Kind).

*   **Verificar el estado de los nodos:**
    Asegúrate de que el nodo o nodos del clúster estén listos:
    ```bash
    kubectl get nodes
    ```

*   **Verificar los Namespaces:**
    El ecosistema implementa tres namespaces diferentes para el aislamiento de entornos (`dev` para ramas de características, `stage` para pruebas/liberaciones y `master` para producción):
    ```bash
    kubectl get namespaces
    ```
    *(Nota: Se pueden aplicar/crear con `kubectl apply -f k8s/namespaces.yml`)*

*   **Listar los Pods de Infraestructura:**
    Para verificar el estado de los componentes de persistencia y mensajería (PostgreSQL, Redis, Neo4j, Zookeeper, Kafka y OpenLDAP) en el namespace de desarrollo (`dev`), ejecuta:
    ```bash
    kubectl get pods -n dev -l "app in (postgres, redis, neo4j, openldap, kafka, zookeeper)"
    ```
    *(Puedes cambiar `-n dev` por `-n stage` o `-n master` según el entorno que estés validando).*

---

## 🧪 3. Ejecución Manual de Pruebas (Gradle)

Las pruebas están organizadas en diferentes suites para validar tanto los comportamientos internos de los microservicios como el flujo extremo a extremo (E2E) y de carga.

### Pruebas Unitarias e Integración
*   **Comando general:**
    Para ejecutar todas las pruebas unitarias y de integración de la solución en local, utiliza:
    ```bash
    ./gradlew test
    ```
*   **Optimización del Pipeline (Exclusión del Gateway):**
    En entornos CI/CD, donde la infraestructura aún no está desplegada en el paso de compilación inicial, se omiten las pruebas del Gateway (que son de tipo E2E y requieren los contenedores activos) mediante el comando:
    ```bash
    ./gradlew test -x :services:circleguard-gateway-service:test --no-daemon --continue
    ```

### Pruebas E2E (REST Assured)
Estas pruebas realizan llamados reales a través del API Gateway para validar flujos completos de negocio (Autenticación, Perfiles, Envío de Formularios, Guardado en Grafos y Seguridad).

*   **Comando de ejecución (apuntando al cluster local Kind - puerto NodePort 30087):**
    ```bash
    ./gradlew test --tests "*E2E*" -Dgateway.url=http://localhost:30087
    ```
*   **Comando de ejecución (apuntando a Docker Compose local - puerto 8087):**
    ```bash
    ./gradlew test --tests "*E2E*" -Dgateway.url=http://localhost:8087
    ```
*   **Nota Técnica:**
    El parámetro `-Dgateway.url` inyecta dinámicamente la propiedad de sistema que lee la suite `SystemWideE2ETest.java` (`System.getProperty("gateway.url", "http://localhost:8087")`).

### Pruebas de Rendimiento (Locust)
Las pruebas de carga simulan el inicio de sesión y envío masivo de formularios de salud estresando los servicios.

*   **Opción A: Levantar Locust Local con Python (Interfaz Web)**
    Puedes ejecutar Locust en tu máquina local levantando su panel web interactivo en `http://localhost:8089`:
    ```bash
    # 1. Crear entorno virtual (opcional)
    python -m venv .venv
    
    # 2. Activar entorno virtual
    # En Windows (PowerShell):
    .venv\Scripts\Activate.ps1
    # En macOS/Linux:
    source .venv/bin/activate

    # 3. Instalar dependencias
    pip install -r performance-tests/requirements.txt

    # 4. Levantar la interfaz de Locust apuntando al Gateway de Kind
    locust -f performance-tests/locustfile.py --host http://localhost:30087
    ```

*   **Opción B: Ejecutar Locust usando Docker (Interfaz Web)**
    Si prefieres usar un contenedor Docker para no instalar dependencias locales:
    ```bash
    docker run --rm -it -p 8089:8089 -v ${PWD}/performance-tests:/mnt/locust locustio/locust -f /mnt/locust/locustfile.py --host http://host.docker.internal:30087
    ```
    *(Luego ingresa a `http://localhost:8089` en tu navegador para configurar y lanzar la prueba).*
