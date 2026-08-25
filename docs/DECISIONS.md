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

---

## ADR-011 — Comunicación backend ↔ recon_modules

Status: Accepted

Decisión:
Comunicación HTTP/REST asíncrona entre Spring Boot y Flask (Python).

Patrón:
Async con job ID + polling + callback.

Flujo:
1. Backend recibe request del frontend → crea ScanJob en DB (`status=PENDING`)
2. Backend → `POST http://modules:5000/scan` {target, config, nvdApiKey}
3. Flask genera jobId, lanza pipeline en hilo separado, retorna 202
4. Backend guarda jobId en ScanJob, retorna 202 al frontend
5. Frontend polls `GET /api/auditorias/{id}/scan/status`
6. Flask ejecuta: port_scan → service_scan → NVD/KEV/EPSS lookup
7. Al terminar, Flask → `POST http://back:8080/api/internal/scans/{jobId}/result`
8. Backend persiste en PostgreSQL, invalida caché Redis

Contrato de datos:

Request (backend → Flask):
```json
{
  "target": "192.168.1.0/24",
  "nvdApiKey": "user-owned-key",
  "config": {
    "timeout": 300,
    "icmpTimeout": 5,
    "maxCveYears": 5,
    "minCvssScore": 5.0
  }
}
```

Response (Flask → backend, en callback):
```json
{
  "hosts": [{ "ip", "mac", "hostname", "os", "ports": [...] }],
  "apiResults": [{ "cpeString", "vulnerabilities": [...], "lastChecked" }],
  "nmapVersion": "...",
  "startTime": "...",
  "endTime": "..."
}
```

Endpoints Flask:
- `POST /scan` → 202 { jobId }
- `GET /scan/{jobId}/status` → 200 { status, progress }
- `GET /scan/{jobId}/result` → 200 { scanResult }

Endpoint backend (callback interno):
- `POST /api/internal/scans/{jobId}/result` → 200

**NVD API key:**
Cada usuario gestiona su propia API key de NVD. La key se almacena
en la tabla usuario (encriptada) y se envía al módulo en cada request
de scan. Esto distribuye el rate limit entre usuarios y respeta el
principio de menor privilegio: el backend solo conoce la key del
usuario autenticado, no una key global.

Restricciones:
- Flask usa threading para no bloquear el event loop de aiohttp
- Timeout configurable por scan (default 600s)
- Si Flask no responde en 10s, backend marca job como FAILED
- Callback al backend es obligatorio (no polling inverso)
- La nvd_api_key del usuario es opcional: sin ella se omite fase 3

Justificación:
Operaciones de escaneo duran 1-5 minutos. Un patrón sync bloquearía
el hilo de Flask y no permitiría mostrar progreso. El patrón async
con job ID es el estándar para operaciones de larga duración (CI/CD,
escaneos, exports). Escala horizontalmente si se agrega más de un
worker de escaneo.

---

## ADR-012 — Modelo de datos de escaneo

Status: Accepted

Decisión:
Introducir la entidad `Escaneo` como nodo central entre Auditoría y
los resultados del scan. Cada ejecución de escaneo se modela como
una entidad propia con relación 1:N con Auditoría y 1:N con Activo.

Modelo:

```
 Auditoría (1) ──── (N) Escaneo ──── (N) Activo ──── (N) Puerto ──── (M:N) Cpe ──── (M:N) Cve ──── (M:N) Cwe
                            │
                            └── objetivos[], estado, progreso, modulo_job_id, ...
```

Entidad Escaneo:
- Representa una ejecución individual de Nmap + NVD lookup
- Agrupa todos los hosts resultantes de una llamada al módulo
  - Una llamada puede cubrir un CIDR (192.168.1.0/24) o múltiples IPs (192.168.2.5, 192.168.3.4). Nmap soporta ambos
- Tiene su propio ciclo de vida: PENDING → RUNNING → COMPLETED/FAILED

Cambios en el DER:

Nuevas tablas:
- Escaneo (escaneo_id, auditoria_id FK, objetivos TEXT[], estado, progreso, modulo_job_id, nmap_version, mensaje_error,
  iniciado_a, completado_a, creado_a, resultado_jsonb)

Tablas modificadas: solo hay cambios en:
- Activo: auditoria_id FK → escaneo_id FK (la relación ahora es con Escaneo, no directamente con Auditoría)
- Auditoria: eliminar campo resultado_jsonb (los resultados viven en Escaneo), eliminar campo nmap_version (ahora en Escaneo)
- Usuario: agregar campo nvd_api_key (VARCHAR, nullable)

Justificación:
Escaneo como entidad propia permite:
- Historial completo de escaneos por auditoría
- Comparación temporal de resultados (**T8: dashboard**)
- Cada escaneo tiene su propio status, timestamps y targets
- `Activo.auditoria_id` se reemplaza por `Activo.escaneo_id`, manteniendo la trazabilidad de cuándo se descubrió cada host
- Los datos de referencia (Cpe, Cve, Cwe) se comparten entre escaneos, no se duplican