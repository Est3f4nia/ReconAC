# ReconAC — Tasks

Registro de tareas aprobadas para implementación.

## Estados

- `PROPOSED` → propuesta pendiente de aprobación.
- `APPROVED` → aprobada por el Project Owner.
- `IN_PROGRESS` → en implementación.
- `REVIEW` → implementación terminada, pendiente de revisión.
- `DONE` → aprobada y finalizada, lista para prod.
- `BLOCKED` → bloqueada por una decisión o dependencia.

---

## Tareas

### Estructura

Status: PROPOSED

Título:
[Nombre de la tarea]

Meta:
[Qué se quiere conseguir]

Requerimientos:
- [Requisito]
- [Requisito]

Restricciones:
- [Restricción]

Componentes afectados:
- [Componente]

Notas:
[Información adicional]

---

### Reglas

- Solo implementar tareas `APPROVED`.
- OpenCode puede modificar el estado a `IN_PROGRESS` y `REVIEW`.
- El PO decide cuándo una tarea pasa a `DONE`.
- Si durante la implementación aparece un conflicto arquitectónico, marcar la tarea como `BLOCKED` y consultar al PO.
- Las tareas se escriben sobre este archivo, sin borrar o modificar directrices.

---

*(Insertar tareas como H3 a partir de acá abajo)*

---

### T1 — Migración SQL del esquema completo

Status: REVIEW (terminada, pero falta testeo de DB con el back en funcionamiento, se queda en REVIEW por prevención)

Título:
Migración Flyway del esquema relacional completo

Meta:
Crear migración SQL que implemente el DER actualizado con todas las tablas y relaciones definidas.

Requerimientos:
- Crear tablas: Usuario, Auditoria, Activo, Puerto, Puerto_Cpe, Cpe, Cpe_Cve, Cve, Referencia, Cve_Cwe, Cwe
- Implementar ENUM estado_puerto_enum
- Crear constraints: PKs, FKs, UNIQUE, CHECK, NULLABLE donde corresponda
- Crear índices para queries frecuentes
- Eliminar tabla Servicio (no existe en el DER actual)
- Incluir campos: servicio_nombre en Cpe, servicio_fallback en Puerto, cwe_code en Cwe, TEXT[] en Referencia.tags

Restricciones:
- Compatible con PostgreSQL
- Migración única V1 (no hay esquema previo que preservar)
- Usar UUID para PKs

Componentes afectados:
- `recon_back`

---

### T2 — Configuración de Redis + estrategia de caché

Status: REVIEW (terminada, pero falta testeo de DB con el back en funcionamiento, se queda en REVIEW por prevención)

Título:
Implementar caché Redis con Read-Through y Write-Around

Meta:
Configurar Redis en Spring Boot y construir la capa de caché para tablas de referencia.

Requerimientos:
- Configurar conexión a Redis en application.properties
- Implementar Read-Through: consultar Redis → miss → PostgreSQL → almacenar en Redis con TTL 24h
- Implementar Write-Around: persistir en PostgreSQL → invalidar key de Redis
- Cachear: Cpe, Cve, Cwe, sus junction tables (Cpe_Cve, Cve_Cwe), y Referencias
- Usar Cpe.ultimo_check como guard para evitar reconsultas a APIs externas

Restricciones:
- Redis como servicio independiente del lenguaje
- TTL configurable
- No cachear tablas de auditoría (Usuario, Auditoria, Activo, Puerto)

Componentes afectados:
- `recon_back`

---

### T3 — API REST: Auditoría y Activos

Status: REVIEW (terminada, pero falta testeo de DB con el back en funcionamiento, se queda en REVIEW por prevención)

Título:
CRUD de gestión de auditorías y activos

Meta:
Implementar los endpoints REST para la gestión de auditorías y activos. Los puertos son detectados por Nmap en el escaneo, no se gestionan manualmente.

Requerimientos:
- Endpoints CRUD para Auditoria y Activo
- Separación en capas: Controller → Interfaz → Service → Repository
- Paginación en listados
- DTOs como record, validación estricta con jakarta (input de usuario)
- Manejo de errores con respuestas HTTP apropiadas (@RestControllerAdvice, GlobalExceptionHandler). Respetar la estructura del paquete `exceptions/` (`global/` para excepciones genéricas y/o reutilizables, el resto para excepciones específicas y aisladas de cada feature). Cumplimiento de RFC 9457 y homogenización de respuestas (`exceptions/CustomException/` extends `RuntimeException`)
- Implementación de `/config/BaseResponse/`para homogenizar respuestas

Restricciones:
- Seguir convenciones Spring Boot (paquete `feature/`)
- UUID para IDs
- Coherencia con el esquema de la T1

Componentes afectados:
- `recon_back`

#### T3.1 — API REST: JWT, buenas prácticas, evaluación de implementación

*Insertada manualmente por el PO, por conveniencia, la estructura es distinta en este tipo de inserciones*

Status: REVIEW (terminada, pero falta testeo de DB con el back en funcionamiento, se queda en REVIEW por prevención)

Título:
Review de implementación de T3

Meta:
Enriquecer y corregir funcionalidades

> Contexto: API compila en el estado actual, aún así, existe deuda técnica y problemas graves de implentación.

**Bugs y problemas:**

1. `ActivoUpdateService` no persiste (`ActivoUpdateService.java:24-29`)

```java
ActivoMapper.updateEntity(activo, req);
return ActivoMapper.toResponse(activo);
// Falta repo.save(activo)
```

Solo funciona por dirty checking de JPA en `@Transactional`, pero es frágil e implícito. Si alguien saca `@Transactional` o cambia el isolation level, los updates se pierden silenciosamente.

2. `AuditoriaRequestDto` — pide ID de usuario (`AuditoriaRequestDto.java:12`)

El campo no es necesario con la implementación de JWT, la auditoría debe crearse "sobre" una sesión válida.

2-2. `AuditoriaCreateService` ignora el usuario autenticado (`AuditoriaCreateService.java:15`)

```java
// Usuario usuario = validateUser.getAuthenticatedUserSession();
```

El `usuarioId` viene en el request DTO. Cualquier usuario autenticado puede crear auditorías a nombre de cualquier otro usuario. Esto es un problema de seguridad serio.

3. `ActivoGetService` — sin paginación (`ActivoGetService.java:20`)

```java
return repo.findAll().stream()...
```

El repo declara `findByAuditoriaId(UUID, Pageable)` pero el service usa `findAll()`. Con muchos activos esto es un problema de rendimiento y de memoria.

**Deuda técnica y malas prácticas:**

1. No hay paginación en endpoints — Requisito de T3: "Paginación en listados". No está implementada en ningún controller. Los repos la tienen pero nada la usa.

2. Roles hardcodeados — Usuario.getAuthorities() devuelve siempre `ROLE_USER`. El JWT extrae roles pero el claim se ignora al reconstruir la sesión. No hay escalabilidad para roles futuros. Se planea agregar `ROLE_ADMIN`, con privilegios **no destructivos** de administrador.

3. Refresh token sin rotación — Cada `/refresh` valida el token viejo y devuelve uno nuevo, pero el viejo sigue siendo válido hasta expirar. Un token robado puede refrescarse indefinidamente. Fallo de seguridad (saltear y luego discutir con el PO, la solución a esto implica desición sobre la arquitectura).

4. CORS hardcodeado — `localhost:3000` no es configurable por profile. En producción o Docker habrá que cambiar el código.

5. BaseResponse sin error body — BaseResponse solo tiene errors: `List<String>` pero `ok()` siempre setea `errors=null`. No hay un factory method para errores. El handler usa ProblemDetail directamente, lo cual es correcto, pero la Response DTO no refleja eso.

6. `so_probab` sin validación en DTO — La DB tiene `CHECK (so_probab BETWEEN 0 AND 100)` pero el DTO no valida esto. Un request con `soProbab: 200` llega hasta la DB y falla con un error genérico en vez de un 400 claro.

**Errores de seguridad (deuda técnica T7) — RESUELTOS:**

1. Rate limiting — `RateLimitFilter` (Bucket4j in-memory) sobre `/api/auth/*` por IP. Propiedades en `app.ratelimit.*`. Para réplicas múltiples convendría respaldar en Redis (mejora futura).

2. CSRF — patrón Double Submit Cookie implementado (`CsrfFilter` + cookie `XSRF-TOKEN` no-httpOnly emitida en login/refresh). Se mantiene `csrf` deshabilitado en Spring (lo reemplaza este filtro). Aplica solo a sesiones cookie; Bearer queda exento.

3. Headers de seguridad — configurados en `SecurityConfig.headers(...)`: `HSTS`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'`.

---

### T4 — Refactor de recon_modules: de CLI a servicio HTTP

Status: REVIEW

Título:
Transformar recon_modules de CLI a servicio Flask con API REST

Meta:
Exponer el pipeline de reconocimiento como API HTTP que el backend
consume según **ADR-011**. El módulo deja de ser una CLI y pasa a ser
un servicio independiente.

Requerimientos:

**API Flask:**
- Crear Flask app con endpoints: `POST /scan`, `GET /scan/{id}/status`, `GET /scan/{id}/result`
  - `POST /scan` acepta JSON con target, nvdApiKey y config, retorna 202 con jobId
    - Jobs se ejecutan en hilo separado (`threading.Thread`)
  - `GET /status` retorna estado del job (PENDING/RUNNING/COMPLETED/FAILED) y progreso ("Phases" especificadas en `reconac.py`)
  - `GET /result` retorna el JSON completo del escaneo solo si `status=COMPLETED`
- Al finalizar, Flask hace POST callback al backend (url configurable via env)
- Manejo de errores con respuesta JSON consistente

**Pipeline refactorizado:**
- Extraer pipeline de `reconac.py` como función reutilizable `run_scan(config) → dict`
- Mantener las 3 fases: port_scan → service_scan → NVD/KEV/EPSS lookup
- Desconectar fase 4 (reportes MD/CSV), es responsabilidad del backend.
  - IMPORTANTE: el código se eliminará una vez terminada la integración (T5). Se lo deja para tener en cuenta estructura y presentación de datos.
- Eliminar dependencia de `argparse` y `CLI` de `reconac.py`
- Mantener `scanning/` y `cves/` sin cambios funcionales internos

**Serialización:**
- Convertir dataclasses (ScanResult, ApiResult, etc.) a diccionarios con `asdict()`
- Fechas datetime → ISO 8601 string
- Listas y Optional se serializan nativamente

**Configuración:**
- Crear ``requirements.txt`` con dependencias declaradas (flask, aiohttp, yarl)
- `NVD_API_KEY` se recibe en cada request de scan (key por usuario, **no global**)
- Configurar host/port/flask env via variables de entorno
- Agregar `__init__.py` donde haga falta

**Manejo de errores:**
- Errores de Nmap → job `status=FAILED` con mensaje descriptivo
- Errores de NVD API → job completado parcialmente (hosts sin CVEs)
- Target inalcanzable → warning en resultado, no error fatal. No se debe ejecutar el escaneo (comprobación de conectividad antes de ejecutar Nmap)
- Todos los errores se retornan como JSON con campo "error"

Restricciones:
- Python 3.10+
- No romper funcionalidad de escaneo existente
- Mantener modularidad interna (`scanning/`, `cves/`, `models/`)
- El pipeline debe seguir siendo async internamente (aiohttp para NVD)

Componentes afectados:
- `recon_modules`

Nota:
- Es CRÍTICO mantener la restricción de fetching de CPEs genéricas.

Notas de implementación (estado actual, alineado a ADR-011 revisada):
- El módulo ahora NOTIFICA al backend (no solo trackea en memoria):
  - Al iniciar el hilo: `POST {BACKEND_API_URL}/api/internal/scans/{scan_id}/status` (RUNNING).
  - Al terminar OK: `POST .../status` (COMPLETED) + `POST .../callback` con el resultado
    mapeado al contrato `EscaneoResult` (hosts con ip/mac/hostname/os, nmapVersion, startTime, endTime).
  - Al fallar: `POST .../status` (FAILED, con error).
- `run_scan` ahora devuelve `ScanReport` (ScanResult + ApiResult) para conservar el
  inventario de hosts/puertos que antes se descartaba.
- Variables de entorno del módulo: `BACKEND_API_URL` (default `http://localhost:8080`,
  en Docker `http://recon_back:8080`), `NVD_API_KEY` (fallback; el backend ya envía la key
  por usuario). Se agregó `requests` a requirements.txt para los callbacks.
- El backend expone los endpoints internos en `/api/internal/scans/{jobId}/status` y
  `/callback` (sin JWT, permitidos en SecurityConfig).

---

### T5 — Integración backend ↔ recon_modules

Status: REVIEW

Título:
Backend invoca escaneo via HTTP y persiste resultados

Meta:
El backend envía targets al módulo Python vía HTTP (**ADR-011**),
recibe resultados y los persiste en PostgreSQL siguiendo el
modelo de datos de **ADR-012**, usando caché Redis para tablas
de referencia.

Requerimientos:

**Migración V2:**
- Crear tabla Escaneo (escaneo_id, auditoria_id FK, objetivos TEXT[], estado, progreso, modulo_job_id, nmap_version, mensaje_error,
  iniciado_a, completado_a, creado_a, resultado_jsonb)
  - Estados: PENDING, RUNNING, COMPLETED, FAILED
  - Relación 1:N con Auditoría y 1:N con Activo
- Alterar tabla Activo: reemplazar auditoria_id FK → escaneo_id FK
- Alterar tabla Auditoria: eliminar resultado_jsonb y nmap_version
- Alterar tabla Usuario: agregar `nvd_api_key (VARCHAR, nullable)`
  - La key es opcional: sin ella se omite la fase de lookup de CVEs, se debe informar que sin ella al escaneo le faltará información.
  - El backend lee la key del usuario autenticado y la pasa al módulo

**Controller de escaneo:**
- `POST /api/auditorias/{id}/scan` → recibe objetivos[], crea Escaneo (status=PENDING), envía al módulo
- `GET /api/auditorias/{id}/scan/{scanId}/status` → retorna estado del Escaneo
- Endpoint interno `POST /api/internal/scans/{scanId}/result` → callback del módulo

**Service de escaneo:**
- `ScanService`: coordina creación de Escaneo, llamada HTTP al módulo, persistencia
- Usa RestTemplate o WebClient para llamar a Flask
- Timeout configurable para la llamada HTTP
- Si el módulo no responde, marca Escaneo como FAILED

**Persistencia de resultados:**
- Recorrer hosts del scan → crear/actualizar Activo
- Recorrer puertos → crear Puerto, vincular con CPEs via Puerto_Cpe
- Recorrer apiResults → upsert Cpe, Cve, Cwe, Referencia
- Usar `CacheService` para tablas de referencia (Cpe, Cve, Cwe)
- Actualizar `Cpe.ultimo_check` tras cada escaneo
- Transacciones: todo el persist dentro de `@Transactional`

**Manejo de errores:**
- Si el callback falla, el Escaneo queda en FAILED con error message
- Si un host no tiene puertos, se guarda como Activo sin puertos
- Si NVD no retorna CVEs, el Cpe se guarda sin vulns
- No crashear el backend por errores del módulo

Restricciones:
- Comunicación back → modules via HTTP interno (no subprocess)
- Coordinar con **ADR-010** (Docker Compose, hostname "modules")
- NVD_API_KEY viene del usuario autenticado, no es global
- Los puertos se gestionan exclusivamente por escaneo (no manual)

Componentes afectados:
- `recon_back`

Notas de implementación (estado actual, alineado a ADR-011 revisada):
- Backend expone: `POST /api/auditorias/{auditoriaId}/escaneos` (inicia),
  `GET /api/auditorias/{auditoriaId}/escaneos/{escaneoId}/status` (progreso),
  `DELETE /api/auditorias/{auditoriaId}/escaneos/{escaneoId}` (borrado real).
- Backend expone endpoints internos solo para Flask (sin JWT):
  `POST /api/internal/scans/{jobId}/status` y `POST /api/internal/scans/{jobId}/callback`.
- El backend NO consulta NVD/KEV/EPSS: lo hace Flask. `HttpConfig` fue eliminado
  (los beans NIST/EPSS/KEV no corresponden al backend).
- `Escaneo.estado` es ahora `EscaneoEstado` (enum: QUEUED/RUNNING/COMPLETED/FAILED),
  coherente con `estado_escaneo_enum` de V2.
- `nvd_api_key` se valida con `KeyNotValidException` (unicidad, sin data leakage) en
  `UsuarioKeyService`.
- El callback persiste `resultado` JSONB y crea `Activo` por host detectado. La migración
  de `Puerto`/`Cpe`/`Cve` queda pendiente hasta que esas entidades existan como JPA.
- La NVD API key del usuario se almacena **hasheada (SHA-256)** en `usuario.nvd_api_key`
  (unicidad, sin plaintext). En cada scan el frontend envía la key en `ScanStartRequest.nvdApiKey`;
  el backend la valida contra el hash (`isUserKey`) y la **retransmite a Flask** en `POST /scan`
  (`nvd_api_key`). NO existe key global: cada usuario usa su propia key (límite de NVD por key).
  Si el usuario no envía su key, el scan se hace sin enriquecer CVEs y se le informa. Ver ADR-011.
- **Carrera benigna (conocida):** Flask notifica `RUNNING`/`COMPLETED`/`FAILED` al backend por
  callback. El estado `RUNNING` a veces puede no persistirse si Flask notifica antes de que el
  backend guarde `modulo_job_id` (el backend lo guarda justo tras recibir el 202 de Flask). El
  estado final `COMPLETED`/`FAILED` siempre llega porque Nmap tarda segundos. Para eliminarla
  del todo se podría generar el `jobId` en el backend y pasarlo a Flask en lugar de que Flask lo
  genere; se deja como mejora pendiente.
- Config de endpoints de módulos externalizada como mapa (`ModulesConfig` +
  `ModuleEndpoint`, `@ConfigurationProperties` `app.modules.*`). ADR-013: Opción A
  (mapa por entorno). Opción B (registro en DB/Redis, afecta ADR-010) queda
  comentada en `EscaneoCreateService` como mejora futura. `app.modules.api-url`
  fue reemplazado por `app.modules.recon.url`.

---

### T6 — Integración APIs externas (NVD, KEV, EPSS)

> ⚠️ Conflicto con ADR-011 revisada: la decisión ahora establece que Flask (recon_modules)
> ejecuta las consultas a NVD/KEV/EPSS, no el backend. T6 debe reescribirse para reflejar que
> el backend solo orquesta y persiste, y que las consultas externas viven en el módulo Python.
> Pendiente de decisión del PO.

Status: REVIEW

Título:
Consultas a APIs externas de vulnerabilidades

Meta:
Implementar en el backend las consultas a NVD, CISA KEV y EPSS con caché.

Requerimientos:
- Consulta a NVD API v2.0 (CPE → CVE)
- Consulta a CISA Known Exploited Vulnerabilities Catalog
- Consulta a EPSS (Exploit Prediction Scoring System)
- Paginación y rate limiting en requests
- Caché de resultados en Redis
- Filtrado por CVSS, antigüedad y otros criterios

Restricciones:
- Requiere NVD API key
- Respetar rate limits de cada API
- Integrar con la estrategia de caché de la T3

Componentes afectados:
- `recon_back`

Notas:
- Luego de implementación ¿cómo administrar grandes cantidades de CVEs? ¿Restrición de objetos? ¿Paginación?

---

### T7 — Autenticación y seguridad

Status: REVIEW

Título:
Implementar autenticación, autorización y headers de seguridad

Meta:
Proteger la API con JWT, CSRF, CORS y headers de seguridad.

Requerimientos:
- Login y registro de usuarios
- JWT con httpOnly cookies
- CSRF protection (Double Submit Cookie o Spring CSRF)
- CORS configurado solo para el frontend
- Headers de seguridad: HSTS, X-Content-Type-Options, X-Frame-Options, CSP, Referrer-Policy
- Rate limiting con Bucket4j
- Bean Validation en inputs

Restricciones:
- Spring Security
- HTTPS en producción
- Coherencia con esquema de Usuario en la T1

Componentes afectados:
- `recon_back`

Notas de implementación (REVIEW):
- Auth/registro/login/refresh: ya existían (JWT en cookies `HttpOnly`+`Secure`+`SameSite=Strict`).
- CSRF: `CsrfFilter` (Double Submit Cookie) + cookie `XSRF-TOKEN` emitida en login/refresh; el SPA debe reenviarla en header `X-XSRF-TOKEN` en requests mutantes.
- CORS: `CorsConfig` (origen del frontend).
- Headers: `SecurityConfig.headers(...)` → HSTS, nosniff, X-Frame-Options DENY, Referrer-Policy no-referrer, CSP restrictiva.
- Rate limiting: `RateLimitFilter` (Bucket4j in-memory, por IP) en `/api/auth/*`; props `app.ratelimit.*`.
- Bean Validation: `@Valid` + `@NotBlank/@Email/@Size` en DTOs + handler 400 (`MethodArgumentNotValidException`). Ya cubierto.
- Pendiente de pruebas manuales del PO (flujo CSRF desde el SPA y headers en prod/HTTPS).

---

### T8 — Frontend React

Status: PROPOSED

Título:
Desarrollo de la interfaz de usuario con React

Meta:
Implementar la GUI completa según los requisitos del formulario 1.

Requerimientos:
- Portales de login y registro
- Interfaz de escaneo (crear auditoría, agregar activos, ejecutar)
- Interfaz de gestión de activos (trazabilidad)
- Dashboard de proyecto:
  - KPIs: promedio CVSS, cantidad CVEs críticas
  - Historial parcial y completo de escaneos
  - Timeline de nivel general de riesgo
  - Desglose de CVEs: más comunes, con explotación activa, más criticidad
  - Desglose de hosts: con más vulns crítsicas
  - Comparación entre ejecuciones sobre mismo activo
- Exportación de reportes (MD, CSV)

Restricciones:
- Consumir la API del backend (T4, T7)
- CORS solo front
- Última tarea en orden de dependencia

Componentes afectados:
- `recon_front`

---

### T9 — Containerización con Docker Compose

Status: PROPOSED

Título:
Levantar el entorno completo con Docker Compose

Meta:
Containerizar todos los servicios para que el entorno de desarrollo
y producción sea reproducible sin dependencias manuales del host.

Requerimientos:
- Dockerfile para recon_back (Java 21, Spring Boot)
- Dockerfile para recon_modules (Python 3.10+, Nmap, Flask)
- Dockerfile para recon_front (multi-stage: Node → Nginx)
- docker-compose.yml con todos los servicios
- Variables de entorno via .env (DB credentials, NVD API key)
- recon_modules con `cap_add: NET_RAW` para SYN scan
- Comunicación back → modules via HTTP interno (Flask, puerto 5000)
- Volumen persistente para PostgreSQL (pgdata)

Restricciones:
- Nmap incluido en la imagen Python (no en el host)
- Redis y PostgreSQL usan imágenes oficiales
- Front solo expone puerto 3000 al host

Notas:
Corresponde a ADR-010. Conviente implementar después de T1-T4
(base funcional) pero antes de T5 (integración back ↔ modules),
ya que la comunicación entre contenedores cambia de subprocess a HTTP.

Componentes afectados:
- `recon_back`, `recon_modules`, `recon_front`
