#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ACTION=${1:-}
shift
if [ "$#" -gt 0 ]; then
    echo 'Uso: sh start-reconac.sh | sh stop-reconac.sh' >&2
    exit 1
fi
command -v docker >/dev/null 2>&1 || { echo 'Instalar Docker Desktop o Engine con Compose.' >&2; exit 1; }
OS=$(docker info --format '{{.OSType}}')
[ "$OS" = linux ] || { echo 'Se requieren contenedores Linux.' >&2; exit 1; }
VERSION=$(docker compose version --short)
VERSION=$(printf '%s' "$VERSION" | sed 's/^v//; s/[-+].*$//')
if ! printf '%s\n' "$VERSION" | awk -F. 'NF == 3 && ($1 > 2 || ($1 == 2 && ($2 > 24 || ($2 == 24 && $3 >= 4)))) {ok=1} END {exit !ok}'; then
    echo 'Se requiere Docker Compose 2.24.4 o posterior.' >&2
    exit 1
fi
unset DB_NAME DB_USER DB_PASSWD JWT_SECRET NVD_ENCRYPTION_KEY SEC_NAME SEC_PASSWD CACHE_TTL_HOURS FRONTEND_PORT API_PORT COOKIE_SECURE CORS_ORIGINS COMPOSE_FILE COMPOSE_PROJECT_NAME COMPOSE_PROFILES COMPOSE_ENV_FILES COMPOSE_REMOVE_ORPHANS
set -- compose --project-name reconac --project-directory "$ROOT" --env-file "$ROOT/.env" -f "$ROOT/compose.yaml"
if [ "$ACTION" = start ]; then
    for folder in recon_back recon_front recon_modules; do
        [ -f "$ROOT/$folder/Dockerfile" ] || { echo "Falta $folder/Dockerfile. Copiar el paquete sobre el repositorio completo." >&2; exit 1; }
    done
    VOLUMES=$(docker volume ls --format '{{.Name}}')
    DATA=new-data
    if printf '%s\n' "$VOLUMES" | grep -qx reconac_postgres_data; then DATA=existing-data; fi
    # UID/GID del usuario: .env no queda propiedad de root en Linux.
    docker run --rm --network none --user "$(id -u):$(id -g)" \
        --mount "type=bind,source=$ROOT,target=/workspace" --workdir /workspace \
        python:3.12-slim-bookworm python scripts/init-env.py "$DATA"
    docker "$@" config --quiet
    docker "$@" up --build --detach --wait --wait-timeout 300
    ADDRESS=$(docker "$@" port frontend 80)
    echo "ReconAC iniciado: http://$ADDRESS"
elif [ "$ACTION" = stop ]; then
    [ -f "$ROOT/.env" ] || { echo 'Restaurar .env antes de bajar Compose; stop no genera secretos.' >&2; exit 1; }
    docker "$@" down --timeout 40
    echo 'ReconAC detenido. PostgreSQL conserva su volumen y .env no cambia.'
else
    echo 'Accion desconocida.' >&2
    exit 1
fi
