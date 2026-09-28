# ReconAC

Aplicación para **automatizar tareas de reconocimiento activo** en auditorías de seguridad.

Ejecuta escaneos sobre objetivos, identifica hosts, puertos abiertos, servicios, productos y versiones y **enriquece los resultados con información de vulnerabilidades** para facilitar su análisis, priorización y contextualización en materia de seguridad.

## Qué aporta

- Reconocimiento activo mediante **Nmap**.
- Asociación de servicios (**CPE**) y vulnerabilidades (**CVE**).
- Enriquecimiento con:
  - **NVD** para vulnerabilidades conocidas.
  - **EPSS** para probabilidad de explotación.
  - **CISA KEV** para explotación conocida.
  - **CWE** para clasificación.
- Historial de escaneos por auditoría.
- Métricas, evolución temporal de riesgo y desglose por CVE y host.
- Exportación de resultados en **Markdown y CSV**.
- API documentada mediante **OpenAPI / Swagger**.

## Arquitectura

| Componente | Función |
| --- | --- |
| `recon_front` | Interfaz web React + TypeScript + Vite |
| `recon_back` | API REST Spring Boot, autenticación, persistencia y orquestación |
| `recon_modules/vulnEnum` | Motor en Python para reconocimiento con Nmap y generación de reportes |
| PostgreSQL | Persistencia de auditorías, escaneos, activos y vulnerabilidades |
| Redis | Caché de CPE |

**La separación entre API, frontend y módulos permite incorporar nuevos módulos de análisis sin acoplarlos a la interfaz o a la capa de persistencia.**

> En la V1, el motor Python mantiene trabajos y logs en memoria y utiliza un único worker. La arquitectura permite extender los módulos, pero el motor actual requiere modificaciones para escalamiento horizontal.

## Ejecución

### Requisitos

- Docker Desktop en modo Linux containers.
- Docker Compose 2.20 o superior.

Desde la raíz del repositorio:

```sh
cp .env.example .env

# !!! Completar recon_back/.env

# Validar configuración
docker compose --env-file recon_back/.env --env-file .env config --quiet

# Primer arranque
docker compose --env-file recon_back/.env --env-file .env up -d --build --wait --wait-timeout 300

# Arranques posteriores
docker compose --env-file recon_back/.env --env-file .env up -d --wait

# Detener
docker compose --env-file recon_back/.env --env-file .env stop
```

## Acceso

| Recurso | Dirección |
| --- | --- |
| Aplicación | http://localhost:3000 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:3000/swagger-ui/index.html |
| Health | http://localhost:8080/actuator/health |

Por defecto, ReconAC se ejecuta como aplicación local y los servicios internos de PostgreSQL, Redis y módulos no se publican.
