# Verificación de API y Docker — 2026-09-12

Trabajo realizado sobre el repositorio real, conservando las modificaciones locales anteriores. No se modificaron contratos ni lógica de vulnerabilidades, CPE/CVE, EPSS/KEV, escaneo o métricas.

## Resultados

| Comprobación | Resultado |
| --- | --- |
| Backend local, Maven package | Correcto; release Java 21 |
| Backend en builder Docker JDK 21 | Correcto; JRE 21 en runtime |
| Suite Maven completa | Bloqueada en testCompile por tests heredados desactualizados; detalle abajo |
| Tests vigentes seleccionados | 14 ejecutados, 0 fallos, 0 omitidos |
| Frontend typecheck y Vite local | Correcto; advertencia existente de chunk >500 kB |
| Frontend build limpio Linux | Correcto después de corregir mayúscula del import CSS |
| Python local y contenedor: imports / pip check | Correcto; Flask /health responde 200 |
| docker compose config --quiet | Correcto con entorno de prueba; falla explícitamente si faltan secretos |
| Construcción Docker | Tres imágenes construidas: backend, modules y frontend |
| Inicio de stack | Cinco servicios healthy |
| PostgreSQL y Flyway | Migraciones V1–V7 aplicadas; reinicio valida esquema sin cambios |
| Spring → Redis | Cliente Lettuce real conectado, PONG y reconexión tras reinicio de Redis |
| Swagger UI | Página renderizada en navegador con operaciones y schemas |
| /v3/api-docs | 200; 29 operaciones documentadas, parámetros completos y referencias resueltas |
| Consumo OpenAPI | openapi-typescript 7.13.0 genera tipos correctamente en archivo temporal |
| Seguridad | API protegida devuelve 401 sin autenticación; Bearer válido funciona |
| Cookies / CSRF | Login y refresh emiten cookies; POST sin CSRF devuelve 403, con CSRF crea auditoría |
| Frontend → Spring | Peticiones reales de API y reportes a través de nginx |
| Spring → Python | Reportes MD y CSV/ZIP generados por Flask y descargados mediante Spring |
| Python → Spring | Helpers reales _notify_status y _notify_callback persistieron un resultado sintético |
| Lookup interno | Respuesta válida con CPEs vacíos; sin consultar NVD ni usar claves reales |
| nginx | nginx -t correcto; /api/internal bloqueado desde entrada pública |
| React SPA | Ruta /register abierta directamente y refrescada en navegador; fallback también comprobado por HTTP |
| Nmap | 7.93 instalado; SYN/OS en dos puertos de 127.0.0.1 del contenedor, sin errores de privilegios |
| Logs finales | Sin errores de arranque Flyway/PostgreSQL/Redis/Spring/Flask/nginx |

La prueba de integración creó un usuario desechable, una auditoría y un registro sintético de escaneo en el volumen de prueba. No invocó el pipeline de reconocimiento; el resultado se envió mediante los helpers Python actuales. La auditoría de prueba fue eliminada por API. No se usaron datos del PostgreSQL local ni claves NVD reales. La comprobación Nmap se limitó a puertos 5000/5001 del propio contenedor, y un ping al servicio backend.

Las advertencias restantes de Spring incluyen open-in-view, recomendaciones de desactivar Swagger en ciertos despliegues y mensajes informativos de descubrimiento de repositorios Redis. Swagger queda habilitado intencionalmente para este entregable. Durante la interrupción deliberada de Redis hubo timeouts/reintentos esperados, seguidos de reconexión y salud UP; no fueron errores del arranque normal.

## Pruebas heredadas fuera de alcance

`mvn test` no llega a ejecutar la suite completa:

- `feature/auth/AuthControllerTest.java` y `AuthServiceTest.java` construyen `RegisterRequestDto` con dos argumentos, aunque el DTO actual exige tres (`apiKey`).
- `modules/vulnEnum/DashboardContractTest.java` utiliza un constructor antiguo de `MetricasAuditoriaService`.

Se dejaron intactos esos archivos. Para comprobar los tests todavía compatibles se usó un POM temporal fuera del repositorio, apuntando a las fuentes actuales y limitando testCompile a:

- `NvdClientRegressionTest`: 4 tests.
- `CpePersistenceRegressionTest`: 4 tests.
- `NvdCacheRegressionTest`: 1 test.
- `NvdLookupRegressionTest`: 4 tests.
- `ScanLogsAuthorizationTest`: 1 test.

La imagen backend usa `maven.test.skip=true` para que estos tests heredados no bloqueen el empaquetado. Esto no equivale a declarar aprobada la suite completa.

## Dependencias y cambios mínimos de ejecución

- Se conserva `springdoc-openapi-starter-webmvc-ui:3.1.1`, ya existente y compatible con Spring Boot 4.1.0.
- Se agrega `spring-boot-starter-actuator`, con versión administrada por Spring Boot.
- Se agrega `gunicorn==23.0.0` en `requirements-docker.txt`, que incluye el requirements original sin modificarlo.
- Nmap, ping y certificados CA se instalan en la imagen Python. curl se usa para salud Java.
- Python lookup pasa a respetar BACKEND_API_URL, igual que los callbacks.
- La cookie XSRF respeta COOKIE_SECURE, igual que las cookies de autenticación.
- Se corrige `dashboardStyle.css` → `DashboardStyle.css` en el import; no cambian estilos.
- Swagger recibe una CSP específica para sus recursos locales. La política de la API se conserva.
- OpenAPI consolida GET/PATCH `/api/auditorias/{id}` con DELETE `/api/auditorias/{auditoriaId}` en una sola plantilla documental. El código y los parámetros de los controllers no cambian.

## Limitaciones preservadas

- Los endpoints internos no autentican JWT ni CSRF por contrato actual: nginx los bloquea y el puerto directo de API se publica solo en loopback.
- Algunos errores del servicio son convertidos en 500 por el manejador genérico existente; no se refactorizó el manejo de errores.
- Flask usa estado de trabajos y logs en memoria; requiere un worker y pierde ese estado al reiniciar. No se añadió ejecución distribuida ni recuperación de escaneos.
- El stack se verificó con HTTP local; HTTPS y la conectividad hacia objetivos de una red real dependen del despliegue.
- No se hicieron escaneos externos ni validaciones de disponibilidad NVD/FIRST/CISA.

## Entorno de verificación que queda iniciado

Proyecto Compose: `reconac-verification`, volumen independiente `reconac-verification_postgres_data`.

- Frontend: http://localhost:13000
- Swagger: http://localhost:13000/swagger-ui/index.html
- OpenAPI: http://localhost:13000/v3/api-docs
- API directa: http://localhost:18080/api

Los secretos de esta prueba se generaron fuera del repositorio, en un archivo privado del workspace; no se copiaron a `.env.example` ni se incluyen aquí. Para detener esta instancia sin resolver variables de Compose se puede usar Docker Desktop y seleccionar el proyecto `reconac-verification`. Para tu instancia habitual, completar el `.env` raíz y ejecutar los comandos de DEPLOYMENT.md (puertos 3000/8080 por defecto).

## Archivos creados

- `.env.example`
- `DEPLOYMENT.md`
- `README.md`
- `VERIFY.md`
- `compose.yaml`
- `recon_back/.dockerignore`
- `recon_back/Dockerfile`
- `recon_back/src/main/java/com/tup/reconac/config/OpenApiConfig.java`
- `recon_front/.dockerignore`
- `recon_front/Dockerfile`
- `recon_front/nginx.conf`
- `recon_modules/.dockerignore`
- `recon_modules/Dockerfile`
- `recon_modules/gunicorn.conf.py`
- `recon_modules/requirements-docker.txt`
- `scripts/verify_stack.py`

## Archivos modificados por esta tarea

- `recon_back/pom.xml`
- `recon_back/src/main/java/com/tup/reconac/config/SecurityConfig.java`
- `recon_back/src/main/java/com/tup/reconac/feature/activo/controllers/ActivoDeleteController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/activo/controllers/ActivoGetController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/activo/controllers/ActivoPatchController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/activo/controllers/ActivoPostController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/auditoria/controllers/AuditoriaDashboardController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/auditoria/controllers/AuditoriaDeleteController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/auditoria/controllers/AuditoriaGetController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/auditoria/controllers/AuditoriaGetReportController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/auditoria/controllers/AuditoriaPatchController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/auditoria/controllers/AuditoriaPostController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/EscaneoDeleteController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/EscaneoGetController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/EscaneoGetListadoController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/EscaneoPatchController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/EscaneoPostController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/ScanInternalController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/controllers/ScanLogsController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/dtos/internal/ScanStartRequest.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/dtos/internal/ScanStatusResponse.java`
- `recon_back/src/main/java/com/tup/reconac/feature/escaneo/dtos/response/EscaneoResult.java`
- `recon_back/src/main/java/com/tup/reconac/feature/usuario/controllers/AuthController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/usuario/controllers/UsuarioApiKeyUpdateController.java`
- `recon_back/src/main/java/com/tup/reconac/feature/usuario/dtos/response/RefreshResponseDto.java`
- `recon_back/src/main/java/com/tup/reconac/modules/vulnEnum/controllers/InternalVulnerabilityController.java`
- `recon_back/src/main/java/com/tup/reconac/modules/vulnEnum/controllers/MetricasAuditoriaController.java`
- `recon_back/src/main/java/com/tup/reconac/modules/vulnEnum/controllers/VulnerabilityGetController.java`
- `recon_back/src/main/java/com/tup/reconac/modules/vulnEnum/dtos/CveDetalleResponse.java`
- `recon_back/src/main/java/com/tup/reconac/modules/vulnEnum/dtos/nvd/NvdLookupRequest.java`
- `recon_back/src/main/java/com/tup/reconac/modules/vulnEnum/dtos/nvd/NvdLookupResponse.java`
- `recon_back/src/main/resources/application.properties`
- `recon_front/src/components/layout/dashboard/DashboardLayout.tsx`
- `recon_modules/cves/api_nist.py`

Las demás modificaciones que ya estaban en el árbol de trabajo no forman parte de este entregable.
