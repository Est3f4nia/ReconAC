# Despliegue local

ReconAC conserva Spring Boot, React/Nginx, Flask/Nmap, PostgreSQL y Redis en Docker.
Solo Nginx publica un puerto, ligado a `127.0.0.1`. El frontend ya usa rutas relativas
`/api/...`; Nginx conserva ese prefijo al enviarlas al backend. Se mantienen las reglas
que bloquean `/api/internal` y las rutas de Swagger.

## Requisitos

- Windows/macOS: Docker Desktop con contenedores Linux y virtualizacion configurada.
- Linux: Docker Engine y el plugin Docker Compose, accesibles por el usuario actual.
- Docker Compose 2.24.4 o posterior, un navegador y PowerShell 5.1+ o shell POSIX.
- Internet para descargar imagenes y dependencias en el primer build.

No se necesita instalar Java, Maven, Node, Python, Nmap, PostgreSQL ni Redis en el host.
Los scripts preparan `.env` en un contenedor temporal de `python:3.12-slim-bookworm`,
la misma imagen base de los modulos. El contenedor no necesita red; Docker puede
descargar su imagen antes de iniciarlo.

## Instalacion nueva

Desde la raiz, en Windows:

```powershell
.\start-reconac.ps1
```

En Linux/macOS:

```sh
sh ./start-reconac.sh
```

Tambien se pueden invocar los scripts por ruta desde otro directorio. No ejecutar
dos inicios simultaneamente. Si Windows bloquea un script descargado, revisar el archivo
y la politica de ejecucion local antes de desbloquearlo.

El inicio valida Docker, el motor Linux y Compose, copia `.env.example` si falta `.env`,
genera `DB_PASSWD`, `JWT_SECRET` y `NVD_ENCRYPTION_KEY` cuando estan vacios y valida
la configuracion antes de construir e iniciar. Cada secreto nuevo contiene 32 bytes
aleatorios codificados en Base64. Los valores existentes no se rotan ni se imprimen.
El script espera hasta 300 segundos a los healthchecks despues del build y muestra la URL.
Si falla, puede dejar servicios encendidos: revisar los logs o ejecutar el script de parada.

El proyecto se llama `reconac`. Los scripts usan exclusivamente `.env` raiz y
`compose.yaml`, sin permitir que las variables homonimas de la consola los reemplacen.
No modifican permanentemente el entorno de la consola. `recon_back/.env.example`
es solo para ejecutar el backend nativamente.

## Migracion de una instalacion existente

Antes de iniciar, completar `.env` raiz con los **valores originales** de `DB_NAME`,
`DB_USER`, `DB_PASSWD`, `JWT_SECRET` y `NVD_ENCRYPTION_KEY`. Trasladar tambien cualquier
valor personalizado de `SEC_NAME`, `SEC_PASSWD`, cache, puerto, CORS y cookies.
Si estaban en `recon_back/.env`, copiarlos a la raiz; Compose ya no lee ese archivo.
`API_PORT` deja de usarse: el backend permanece dentro de la red Docker.

Conservar `.env` junto con los backups de PostgreSQL, especialmente la clave NVD.
Cambiar `DB_PASSWD` en Compose no modifica la contraseña de una base ya inicializada.
Cambiar `DB_USER` tampoco crea un usuario dentro de un volumen existente: recuperar
el usuario y la contraseña con los que se inicializo esa base. El healthcheck de
PostgreSQL prueba una conexion TCP autenticada y una consulta, para impedir que el
backend arranque si esas credenciales no funcionan.
Si existe el volumen `reconac_postgres_data` y faltan secretos, el inicio falla sin
escribir `.env`. Si faltan secretos y existe `recon_back/.env`, tambien se bloquea
para pedir la migracion. Si usabas otro nombre de proyecto/volumen, recuperar esa
configuracion antes de usar los scripts; la comprobacion cubre el nombre original.

El parser admite `NOMBRE=valor` en una linea, comentarios y comillas simples/dobles
sin escapes. Rechaza duplicados, interpolaciones `$` y barras inversas para evitar
interpretaciones distintas de los secretos. Si un secreto existente usa esos caracteres,
**no cambiarlo**: adaptar el parser o usar Compose directamente revisando su interpretacion.
En Linux los archivos nuevos usan el UID/GID del usuario y modo 0600; en Windows los
permisos efectivos dependen de las ACL de la carpeta. No publicar `.env`.

## Acceso, parada y reinicios

- Aplicacion: `http://localhost:3000` o `http://127.0.0.1:3000`.
- API: el mismo origen, bajo `/api`.
- Swagger: `http://localhost:3000/swagger-ui/index.html`.
- Cambiar `FRONTEND_PORT` en `.env` para elegir otro puerto.

`COOKIE_SECURE=false` permite cookies en el HTTP local. CORS admite por defecto
localhost y 127.0.0.1 en el puerto seleccionado. PostgreSQL, Redis, backend y modulos
no tienen puertos publicados. El healthcheck del backend permanece dentro del contenedor.

```powershell
.\stop-reconac.ps1
```

```sh
sh ./stop-reconac.sh
```

La parada ejecuta `stop`: conserva los contenedores, PostgreSQL y `.env`. Requiere
que `.env` siga disponible y valido. Al iniciar de nuevo, `up --build` reutiliza los
contenedores y solo los recrea si cambia su configuracion o imagen.
Redis conserva su comportamiento de cache sin
persistencia. `restart: on-failure:3` limita reintentos de procesos que terminan con
error y no arranca la herramienta al reiniciar Docker; un healthcheck fallido no
reinicia por si solo un proceso vivo.

Para diagnosticar, desde la raiz y sin variables de configuracion contradictorias en la consola:

```sh
docker compose --env-file .env -f compose.yaml -p reconac ps
docker compose --env-file .env -f compose.yaml -p reconac logs --tail 100
```

## Verificacion

Los tests del inicializador se pueden ejecutar en desarrollo con Python 3.12:

```sh
python -m unittest discover -s scripts -p "test_*.py"
```

Para comprobar la precedencia de `.env` y la restauracion de variables en PowerShell:

```powershell
.\scripts\test-deployment.ps1
```

Esta prueba usa Compose real para validar una configuracion temporal y simula los
comandos que requieren el motor; no levanta servicios ni lee el `.env` del usuario.

Probar en cada SO: iniciar sesion, renovar sesion, escribir con CSRF, consultar datos,
acceder a Swagger, escanear un equipo propio y detener/iniciar conservando datos y claves
NVD. Docker Desktop y la red de contenedores pueden modificar la visibilidad ARP/MAC y
el fingerprint de SO de Nmap; conservar `NET_RAW` no elimina esas diferencias de red.

Se conservan los builds locales y Dockerfiles existentes. No se incorporan imagenes
release ficticias ni instaladores nativos. La validacion estatica y del modelo Compose
no reemplaza las pruebas funcionales de contenedores, autenticacion y escaneo.
