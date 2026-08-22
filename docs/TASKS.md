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

## Reglas

- Solo implementar tareas `APPROVED`.
- OpenCode puede modificar el estado a `IN_PROGRESS` y `REVIEW`.
- El PO decide cuándo una tarea pasa a `DONE`.
- Si durante la implementación aparece un conflicto arquitectónico, marcar la tarea como `BLOCKED` y consultar al PO.
- Las tareas se escriben sobre este archivo, sin borrar o modificar directrices.

---

*(Insertar tareas como H3 a partir de acá abajo)*

---

### T1 — Migración SQL del esquema completo

Status: REVIEW

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

Status: REVIEW

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

---

### T4 — Refactor de recon_modules: de CLI a servicio

Status: APPROVED

Título:
Adaptar recon_modules para ejecución como subproceso del backend

Meta:
Transformar la CLI actual en un servicio que acepte input del backend y devuelva resultados estructurados.

Requerimientos:
- Aceptar target, config de escaneo y API keys por argumentos o stdin
- Devolver resultados como JSON por stdout (ScanResult serializado)
- Eliminar generación de reportes (MD/CSV) del módulo — responsabilidad del backend
- Mantener la lógica de Nmap, CPEs y consulta NVD/KEV/EPSS
- Manejo de errores estructurado (exit codes + JSON de error)

Restricciones:
- Python 3.10+
- No romper funcionalidad existente de escaneo
- Mantener modularidad interna (scanning/, cves/, models/)

Componentes afectados:
- `recon_modules`

---

### T5 — Integración backend ↔ recon_modules

Status: PROPOSED

Título:
Invocación de recon_modules como subproceso desde el backend

Meta:
El backend ejecuta el motor de reconocimiento y persiste los resultados en PostgreSQL.

Requerimientos:
- Ejecutar recon_modules como subproceso (ProcessBuilder)
- Pasar target y configuración por argumentos
- Recibir y parsear JSON de stdout
- Persistir resultados: Activos, Puertos, CPEs, CVEs en PostgreSQL
- Usar caché Redis para tablas de referencia (Cpe, Cve, Cwe)
- Actualizar Cpe.ultimo_check tras cada escaneo

Restricciones:
- Timeout configurable para el subproceso
- Manejo de fallos del subproceso (no crashear el backend)
- Coordinación con T4 (endpoints de auditoría)

Componentes afectados:
- `recon_back`, `recon_modules`

---

### T6 — Integración APIs externas (NVD, KEV, EPSS)

Status: PROPOSED

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

---

### T7 — Autenticación y seguridad

Status: PROPOSED

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

---

### T8 — Frontend React

Status: PROPOSED

Título:
Desarrollo de la interfaz de usuario con React

Meta:
Implementar la GUI completa según los requisitos del TPIF.

Requerimientos:
- Portales de login y registro
- Interfaz de escaneo (crear auditoría, agregar activos, ejecutar)
- Interfaz de gestión de activos (trazabilidad)
- Dashboard de proyecto:
  - KPIs: promedio CVSS, cantidad CVEs críticas
  - Historial parcial y completo de escaneos
  - Timeline de nivel general de riesgo
  - Desglose de CVEs: más comunes, con explotación activa, más criticidad
  - Desglose de hosts: con más vulns críticas
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

