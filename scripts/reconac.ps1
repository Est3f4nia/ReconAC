param([ValidateSet('start', 'stop')][string]$Action)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
function Invoke-Docker {
    param([string[]]$DockerArgs)
    & docker @DockerArgs
    if ($LASTEXITCODE -ne 0) { throw "Docker fallo (codigo $LASTEXITCODE)." }
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Instalar Docker Desktop o Docker Engine con Compose.'
}
$os = Invoke-Docker @('info', '--format', '{{.OSType}}')
if (($os | Out-String).Trim() -ne 'linux') { throw 'Activar Docker con contenedores Linux.' }
$versionText = Invoke-Docker @('compose', 'version', '--short')
if (($versionText | Out-String) -notmatch '(\d+)\.(\d+)\.(\d+)') { throw 'No se pudo leer la version de Compose.' }
$version = [version]"$($Matches[1]).$($Matches[2]).$($Matches[3])"
if ($version -lt [version]'2.24.4') { throw 'Se requiere Docker Compose 2.24.4 o posterior.' }
# .env raiz es la fuente de configuracion, incluso si la consola tiene otras variables.
$names = @('DB_NAME','DB_USER','DB_PASSWD','JWT_SECRET','NVD_ENCRYPTION_KEY','SEC_NAME','SEC_PASSWD','CACHE_TTL_HOURS','FRONTEND_PORT','API_PORT','COOKIE_SECURE','CORS_ORIGINS','COMPOSE_FILE','COMPOSE_PROJECT_NAME','COMPOSE_PROFILES','COMPOSE_ENV_FILES','COMPOSE_REMOVE_ORPHANS')
$saved = @{}
try {
    foreach ($name in $names) {
        $saved[$name] = @{
            Exists = [Environment]::GetEnvironmentVariables('Process').Contains($name)
            Value = [Environment]::GetEnvironmentVariable($name, 'Process')
        }
        # En PowerShell reciente, SetEnvironmentVariable($null) puede crear un valor
        # vacio. Compose le da prioridad sobre .env; quitar la entrada por completo.
        Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
    }
    $composeArgs = @('compose', '--project-name', 'reconac', '--project-directory', $root, '--env-file', "$root/.env", '-f', "$root/compose.yaml")
    if ($Action -eq 'start') {
        foreach ($folder in @('recon_back','recon_front','recon_modules')) {
            if (-not (Test-Path "$root/$folder/Dockerfile")) { throw "Falta $folder/Dockerfile. Copiar este paquete sobre el repositorio completo." }
        }
        $volumes = @(Invoke-Docker @('volume', 'ls', '--format', '{{.Name}}'))
        $initArgs = @('run', '--rm', '--network', 'none', '--mount', "type=bind,source=$root,target=/workspace", '--workdir', '/workspace', 'python:3.12-slim-bookworm', 'python', 'scripts/init-env.py')
        if ($volumes -contains 'reconac_postgres_data') { $initArgs += 'existing-data' }
        Invoke-Docker $initArgs
        Invoke-Docker ($composeArgs + @('config', '--quiet'))
        Invoke-Docker ($composeArgs + @('up', '--build', '--detach', '--wait', '--wait-timeout', '300'))
        $address = Invoke-Docker ($composeArgs + @('port', 'frontend', '80'))
        Write-Host "ReconAC iniciado: http://$(($address | Out-String).Trim())"
    } else {
        if (-not (Test-Path "$root/.env")) { throw 'Falta .env. Restaurarlo antes de bajar Compose; stop no genera secretos.' }
        Invoke-Docker ($composeArgs + @('down', '--timeout', '40'))
        Write-Host 'ReconAC detenido. PostgreSQL conserva su volumen y .env no cambia.'
    }
} finally {
    foreach ($name in $saved.Keys) {
        if ($saved[$name].Exists) {
            [Environment]::SetEnvironmentVariable($name, $saved[$name].Value, 'Process')
        } else {
            Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
        }
    }
}
