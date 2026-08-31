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
Comunicación HTTP/REST asíncrona entre Spring Boot (backend) y Flask
(recon_modules). El backend es la única interfaz entre el frontend y
los módulos de reconocimiento.

Patrón:
Async con job ID + callback del módulo al backend + polling del
frontend al backend para progreso.

Responsabilidades por componente:
- **Backend**: única superficie HTTP pública hacia el frontend. Llama a
  Flask para iniciar escaneos, recibe callbacks de estado/resultado,
  persiste en PostgreSQL e invalida la caché Redis de tablas de
  referencia. NO consulta NVD/KEV/EPSS directamente.
- **Flask (recon_modules)**: ejecuta el pipeline (port_scan →
  service_scan → NVD/KEV/EPSS lookup) en un hilo separado. Consulta
  NVD/KEV/EPSS. Lee CPEs de la caché Redis (solo lectura). NO accede a
  PostgreSQL. Al finalizar, notifica al backend vía callback HTTP.
- **Redis**: caché compartida de tablas de referencia (Cpe, Cve, Cwe,
  junction tables). El backend es dueño de las escrituras (Write-Around);
  Flask solo la lee para chequear CPEs ya cacheados. Flask NO escribe en
  Redis (evita envenenamiento de caché y doble dueño).
- **PostgreSQL**: única fuente de verdad. Solo el backend escribe.

Flujo:
1. Frontend → `POST /api/auditorias/{auditoriaId}/escaneos`
   (ScanStartRequest: objetivos[], filtros).
2. Backend crea `Escaneo` (`estado=QUEUED`), valida ownership de la
   Auditoría, y llama a Flask:
   `POST http://modules:5000/scan` { targets, nvd_api_key, timeout,
   icmp_timeout, max_cve_years, min_cvss_score }.
3. Flask genera `scan_id`, lanza pipeline en `threading.Thread`,
   retorna `202 { scan_id, status: "QUEUED" }`.
4. Backend guarda `modulo_job_id = scan_id`, marca `Escaneo.estado=RUNNING`,
   retorna `ScanStatusResponse` al frontend.
5. Flask procesa y, durante la ejecución, informa progreso al backend:
   `POST /api/internal/scans/{jobId}/status` { status, progress, error? }.
6. Frontend hace polling:
   `GET /api/auditorias/{auditoriaId}/escaneos/{escaneoId}/status`
   → `ScanStatusResponse` (estado + progreso).
7. Flask finaliza:
   - Si `FAILED`: `POST /api/internal/scans/{jobId}/status` con
     `status=FAILED` y `error`. No hay callback de resultado.
   - Si `COMPLETED`: `POST /api/internal/scans/{jobId}/status` con
     `status=COMPLETED`, y luego
     `POST /api/internal/scans/{jobId}/callback` con el resultado completo.
8. Backend, en el callback, persiste en PostgreSQL (dentro de
   `@Transactional`): crea/actualiza `Activo` por host detectado,
   `Puerto`, vincula `Cpe` vía `Puerto_Cpe`, upsert `Cpe`/`Cve`/`Cwe`,
   actualiza `Cpe.ultimo_check`, e invalida la caché Redis. Guarda el
   JSON crudo en `Escaneo.resultado`.
9. Frontend, al ver `COMPLETED`, consume
   `GET /api/auditorias/{auditoriaId}/escaneos/{escaneoId}/resultado`.

Contrato de datos:

Request (backend → Flask) — `POST /scan`:
```json
{
  "targets": ["192.168.1.0/24", "10.0.0.5"],
  "timeout": 600,
  "icmp_timeout": 5,
  "max_cve_years": 2,
  "min_cvss_score": 0.0
}
```

Status update (Flask → backend) — `POST /api/internal/scans/{jobId}/status`:
```json
{ "scan_id": "...", "status": "RUNNING", "progress": 40, "error": null }
```

Callback (Flask → backend) — `POST /api/internal/scans/{jobId}/callback`:
```json
{
  "hosts": [{ "ip": "...", "mac": "...", "hostname": "...", "os": "..." }],
  "nmapVersion": "...",
  "startTime": "2026-...",
  "endTime": "2026-..."
}
```

Endpoints Flask:
- `POST /scan` → 202 { scan_id, status }
- `GET /scan/{scan_id}/status` → 200 { status, error }
- `GET /scan/{scan_id}/result` → 200 { scanResult } (uso interno/debug)

Endpoints backend (públicos):
- `POST /api/auditorias/{auditoriaId}/escaneos` → inicia escaneo
- `GET /api/auditorias/{auditoriaId}/escaneos/{escaneoId}/status` → progreso
- `GET /api/auditorias/{auditoriaId}/escaneos/{escaneoId}/resultado` → resultado

Endpoints backend (internos, solo los llama Flask):
- `POST /api/internal/scans/{jobId}/status` → aviso de progreso/fin
- `POST /api/internal/scans/{jobId}/callback` → resultado completo

**NVD API key:**
Cada usuario gestiona su propia NVD API key. **No existe una key global**: las APIs
de NVD imponen límite de consulta por key, por lo que debe ser por usuario para
respetar ese límite (menor privilegio + rate-limit distribuido).

Almacenamiento: se guarda **hasheada (SHA-256)** en `usuario.nvd_api_key`. El backend
NUNCA persiste el plaintext (el hash es irreversible, sirve para unicidad/validación).

Transmisión a Flask: en cada escaneo el frontend envía la key del usuario en el
request (`ScanStartRequest.nvdApiKey`). El backend la valida contra el hash registrado
(`KeyNotValidException`/400 si no corresponde al usuario) y la retransmite a Flask en
`POST /scan` (campo `nvd_api_key`). Flask usa ESA key por usuario para las consultas
NVD. El backend no guarda el plaintext en ningún lado.

Unicidad: el hash es `UNIQUE`; si dos usuarios registran la misma key, el segundo es
rechazado con `KeyNotValidException` sin revelar el motivo (evita data leakage). Si no
se provee key en el scan, Flask omite la fase de lookup de CVEs y el escaneo queda con
menos información (se informa al usuario).

Restricciones:
- Flask usa threading para no bloquear (no se usa polling inverso del backend).
- Timeout configurable por scan (default 600s).
- Si Flask no responde al `POST /scan` en ~10s, backend marca el
  `Escaneo` como `FAILED`.
- Callback al backend es obligatorio para persistir resultados.
- El backend NO consulta NVD/KEV/EPSS: lo hace Flask.
- Flask NO escribe en Redis ni en PostgreSQL.
- Redis es compartida (ADR-004) pero solo el backend escribe.

Justificación:
Operaciones de escaneo duran 1-5 minutos. El patrón async con job ID es
estándar para operaciones de larga duración. Separar responsabilidades
(Backend = persistencia/API, Flask = cómputo, Redis = caché de solo
lectura para Flask) respeta menor privilegio y evita que un módulo
comprometido toque la base o envenene la caché.

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

---

## ADR-013 — Descubrimiento de endpoints de módulos de reconocimiento

Status: Accepted

Contexto:
El backend se comunica con los módulos de reconocimiento (actualmente solo
`recon_modules`, Flask) vía HTTP (ver ADR-011). Con un único `@Value` string
(`app.modules.api-url`) no escala: agregar un módulo nuevo obligaría a tocar el
código de cada servicio que lo invoca.

Decisión:
Se adopta la **Opción A**: externalizar los endpoints como un mapa
`módulo → endpoint` mediante `@ConfigurationProperties`. Cada entorno define las
URL por módulo en `application.properties` / env, sin hardcodear y sin que lo
elija el usuario.

```java
@ConfigurationProperties(prefix = "app.modules")
public class ModulesConfig {
    private Map<String, ModuleEndpoint> endpoints; // "recon" -> {url, timeout}
}
```

Uso en los servicios: `modulesConfig.getEndpoints().get("recon").getUrl()`.
Agregar un módulo = sumar una propiedad (`app.modules.osint.url=...`); no se toca
el código de los call sites.

Formato de config:
```
app.modules.recon.url=${MODULES_API_URL:http://localhost:5000}
app.modules.recon.timeout=30000
```

Consecuencias:
- Escalable a N módulos sin cambios de código en los servicios.
- La URL es config de despliegue, no input de usuario (sin superficie SSRF).
- DRY: si un día se cambia la estrategia de resolución, solo cambia
  `ModulesConfig` / el call site de resolución, no cada servicio.

Futuro — Opción B (NO implementada aún):
Si crece la cantidad de módulos o se requiere descubrimiento dinámico, se puede
implementar un **registro en DB/Redis**: los módulos se auto-registran (estado
`activo`, URL, último heartbeat) y el backend resuelve la URL consultando la tabla
`modulo` (con caché en Redis). Esto extiende la visión de **carga dinámica de
módulos de ADR-010** y requeriría revisarla. La interfaz de resolución
`getModuleUrl(nombre)` se mantiene igual para no alterar los call sites.

Trade-off: Opción B añade infra/complejidad (tabla `modulo`, health-check,
cache). Para el alcance actual (1–2 módulos) la Opción A alcanza y sobra.
