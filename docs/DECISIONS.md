# ReconAC — Architecture Decisións

Registro de Decisiónes arquitectónicas aprobadas.

---

## ADR-001 — Monorepo

Status: Accepted

Decisión:
Meter frontend, backend y módulos de reconocimiento dentro de un único repositorio.

Estructura:

- `recon_back/`
- `recon_front/`
- `recon_modules/`

Justificación:
Facilitar el desarrollo, coordinación y mantenimiento del proyecto completo.

---

## ADR-002 — Backend

Status: Accepted

Decisión:
Utilizar Java + Spring Boot para el backend.

---

## ADR-003 — Database

Status: Accepted

Decisión:
Utilizar PostgreSQL como base de datos principal.

Justificación:
Soporte relacional, integración con Spring Data JPA y soporte de JSON/JSONB.

Flyway será utilizado para gestionar las migraciones.

---

## ADR-004 — Cache

Status: Accepted

Decisión:
Utilizar Redis como mecanismo de cache compartido.

Justificación:
Permitir que distintos componentes puedan utilizar la misma cache.

---

## ADR-005 — Port ≠ Service

Status: Accepted

Decisión:
Puerto y servicio son conceptos independientes y deben mantenerse separados.

Port: representa un endpoint de red identificado por número y protocolo.
Service: representa el programa o servicio que opera sobre un puerto.

No deben fusionarse en una única entidad por conveniencia de implementación.

---

## ADR-006 — CPE como pivote

Status: Accepted

Decisión:
Utilizar CPE como identificador/pivote para relacionar los resultados del reconocimiento con información de vulnerabilidades.

Justificación:
Nmap puede proporcionar CPEs y estos permiten consultar fuentes como NVD.

---

## ADR-007 — "Proyecto" pasa a ser "Auditoría"

Status: Accepted

Decisión:
Cambiar la denominación "Proyecto" por "Auditoría".

Justificación:
Da más claridad a la experiencia de usuario, al referirse como auditoría, queda claro que se refiere a un conjunto de hosts que serán analizados a nivel seguridad.

---

## ADR-008 — Eliminación de tabla Servicio

Status: Accepted

Decisión:
Eliminar tabla Servicio. El nombre del servicio se almacena en
Cpe.servicio_nombre (fuente canónica, cacheada en Redis).
Para puertos sin CPE, Puerto.servicio_fallback almacena el nombre
detectado por Nmap.

Relación:
Puerto →(M:N via Puerto_Cpe)→ Cpe

Justificación:
Servicio duplicaba producto y version de Cpe y solo agregaba el nombre
del servicio. La relación directa via junction table elimina redundancia,
simplifica queries y reduce el total de tablas.

---

## ADR-009 — CWE como entidad independiente

Status: Accepted

Decisión:
CWE se modela como tabla propia (Cwe) con relación M:N con Cve
(via Cve_Cwe). Vocabulario controlado por MITRE.

Justificación:
Permite filtrar CVEs por tipo de debilidad, desglose en dashboard
y clasificación de ataques según lo requerido por el TPIF.

---

## ADR-010 — Containerización

Status: Accepted

Decisión:
Containerizar la aplicación completa con Docker Compose.
El motor de reconocimiento (recon_modules) se containeriza
con Nmap incluido en la imagen, no como dependencia del host.

Setup:
- `recon_back` → imagen Java 21, expone 8080
- `recon_modules` → imagen Python 3.10+ con Nmap, API Flask interna (puerto 5000)
- `recon_front` → multi-stage build (Node → Nginx), expone 3000
- `postgres` → imagen oficial PostgreSQL 16
- `redis` → imagen oficial Redis 7 Alpine

Comunicación entre componentes:
- back → modules: HTTP interno (Flask), no subprocess
- back → PostgreSQL/Redis: por hostname de servicio Docker
- front → back: proxy inverso o CORS según entorno

Restricciones:
- recon_modules necesita `cap_add: NET_RAW` para SYN scan (-sS)
- Nmap se instala en la imagen Python para garantizar portabilidad
- En desarrollo, NVD_API_KEY y credenciales de DB se pasan por .env

Justificación:
Incluir Nmap en el contenedor elimina la dependencia de instalación
manual en el host. En Windows, Nmap no está presente por defecto
y su instalación manual agrega fricción. El contenedor garantiza
que el entorno de ejecución sea reproducible y consistente.