# Instrucciones de Proyecto: Taller 2 - Pruebas y Lanzamiento (DevOps/QA)

Este repositorio es para un entorno académico de alta exigencia centrado en CI/CD, Testing y Kubernetes. Copilot debe seguir estas reglas en cada sugerencia, explicación o generación de código.

## 1. Estándares de Git y Mensajes de Commit
Se debe seguir estrictamente la convención de 'Conventional Commits':
- `feat:` para nuevas funcionalidades (ej. nuevos tests, nuevos endpoints).
- `fix:` para corrección de errores.
- `chore:` para configuración de herramientas (Jenkinsfile, Dockerfile, K8s manifests).
- `test:` para añadir o modificar pruebas (unitarias, integración, E2E, Locust).
- `docs:` para documentación en Markdown o comentarios.

## 2. Estrategia de Ramas (Branching)
- `master`: Entorno de Producción. Solo código estable.
- `develop`: Entorno de Stage. Integración de nuevas funcionalidades.
- `feature/*`: Ramas de desarrollo para tareas específicas.
- Nomenclatura de ramas: `feature/descripcion-breve` o `fix/descripcion-breve`.

## 3. Tecnologías y Herramientas Base
- **CI/CD:** Jenkins (Pipeline as Code mediante Jenkinsfile).
- **Contenerización:** Docker (Multi-stage builds recomendados).
- **Orquestación:** Kubernetes (Manifestos YAML).
- **Pruebas de Rendimiento:** Locust (Python).
- **Microservicios:** Identificar interdependencias y asegurar comunicación fluida.

## 4. Calidad de Código y Naming
- **Variables de Entorno:** UPPER_SNAKE_CASE.
- **Recursos K8s:** kebab-case (ej: auth-service-deployment).
- **Código:** Seguir las mejores prácticas del lenguaje detectado (Node.js, Python, etc.).
- **Idempotencia:** Todos los scripts de despliegue y configuración deben ser idempotentes.

## 5. Objetivo del Taller
Implementar 6 microservicios con pipelines automatizados en 3 entornos (Dev, Stage, Master), incluyendo 5 pruebas unitarias, 5 de integración, 5 E2E y pruebas de estrés/rendimiento con Locust.