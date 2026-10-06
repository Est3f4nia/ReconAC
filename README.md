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

- Docker Desktop con contenedores Linux en Windows/macOS, o Docker Engine en Linux.
- Docker Compose 2.24.4 o superior y un navegador.

Desde la raíz del repositorio, en PowerShell:

```powershell
.\start-reconac.ps1
.\stop-reconac.ps1
```

En Linux/macOS:

```sh
sh ./start-reconac.sh
sh ./stop-reconac.sh
```

El primer inicio crea `.env` raíz y genera los secretos requeridos. **Si ya usabas
ReconAC, migrá primero las credenciales y claves originales a ese archivo**.
`recon_back/.env.example` queda exclusivamente para ejecución nativa.

La parada conserva la base de datos. Consultá [la guía de despliegue local](DEPLOYMENT.md)
para migración, requisitos, diagnóstico y limitaciones de Nmap por sistema operativo.

## Acceso

| Recurso | Dirección predeterminada |
| --- | --- |
| Aplicación | http://localhost:3000 |
| API | http://localhost:3000/api |
| Swagger UI | http://localhost:3000/swagger-ui/index.html |

Solo el frontend publica un puerto, ligado a `127.0.0.1`. Nginx redirige `/api` al
backend y mantiene bloqueadas las rutas internas. Los healthchecks se ejecutan dentro
de los contenedores. `FRONTEND_PORT` permite cambiar el puerto local.

## Licencia

ReconAC is licensed under the
[PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/).

Copyright © 2026 Estefanía Villarreal.
Commercial use is not permitted without prior written permission.
