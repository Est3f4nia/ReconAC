$ErrorActionPreference = 'Stop'
$source = Split-Path -Parent $PSScriptRoot
$root = Join-Path ([IO.Path]::GetTempPath()) ("reconac-env-test-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $root, "$root/scripts", "$root/docker-config" | Out-Null
Copy-Item "$source/scripts/reconac.ps1" "$root/scripts/reconac.ps1"
Copy-Item "$source/compose.yaml" "$root/compose.yaml"
# Credenciales ficticias: no se arranca ningun servicio ni se lee el .env del usuario.
$testKey = [Convert]::ToBase64String([byte[]]::new(32))
@"
DB_NAME=reconac
DB_USER=reconac
DB_PASSWD=test-password
JWT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
NVD_ENCRYPTION_KEY=$testKey
FRONTEND_PORT=3000
"@ | Set-Content -LiteralPath "$root/.env" -Encoding ascii
foreach ($folder in @('recon_back','recon_front','recon_modules')) {
    New-Item -ItemType Directory -Force "$root/$folder" | Out-Null
    Set-Content "$root/$folder/Dockerfile" 'FROM scratch'
}
$global:reconacTestDocker = (Get-Command docker -CommandType Application | Select-Object -First 1).Source
$global:reconacTestConfig = "$root/docker-config"
$global:reconacTestFailUp = $false
$global:reconacTestConfigs = 0
function docker {
    $global:LASTEXITCODE = 0
    $line = $args -join ' '
    if ($line -eq 'info --format {{.OSType}}') { 'linux' }
    elseif ($line -eq 'compose version --short') { '2.24.4' }
    elseif ($line -eq 'volume ls --format {{.Name}}') { 'reconac_postgres_data' }
    elseif ($line -match '^run ') { }
    elseif ($line -match 'config --quiet$') {
        foreach ($name in @('DB_NAME','DB_USER','DB_PASSWD','JWT_SECRET','NVD_ENCRYPTION_KEY','COMPOSE_PROJECT_NAME')) {
            if ([Environment]::GetEnvironmentVariables('Process').Contains($name)) {
                throw "Variable todavia presente durante Compose: $name"
            }
        }
        # Solo la validacion real: no se necesita motor Docker ni se levantan servicios.
        & $global:reconacTestDocker --config $global:reconacTestConfig @args
        $global:reconacTestConfigs++
    }
    elseif ($line -match 'up --build' -and $global:reconacTestFailUp) { $global:LASTEXITCODE = 42 }
    elseif ($line -match 'port frontend 80$') { '127.0.0.1:3000' }
}
$original = @{}
foreach ($name in @('DB_NAME','DB_USER','DB_PASSWD')) {
    $original[$name] = @{
        Exists = [Environment]::GetEnvironmentVariables('Process').Contains($name)
        Value = [Environment]::GetEnvironmentVariable($name,'Process')
    }
}
try {
    Remove-Item Env:DB_NAME -ErrorAction SilentlyContinue
    [Environment]::SetEnvironmentVariable('DB_USER', 'wrong-console-value', 'Process')
    [Environment]::SetEnvironmentVariable('DB_PASSWD', '', 'Process')
    & "$root/scripts/reconac.ps1" -Action start
    if ([Environment]::GetEnvironmentVariables('Process').Contains('DB_NAME')) { throw 'Absent variable not restored' }
    if ($env:DB_USER -ne 'wrong-console-value') { throw 'Existing variable not restored' }
    if ([Environment]::GetEnvironmentVariable('DB_PASSWD', 'Process') -ne '') { throw 'Empty variable not restored' }
    $global:reconacTestFailUp = $true
    $failed = $false
    try { & "$root/scripts/reconac.ps1" -Action start } catch { $failed = $true }
    if (-not $failed) { throw 'Docker failure ignored' }
    if ([Environment]::GetEnvironmentVariables('Process').Contains('DB_NAME')) { throw 'Absent variable not restored after failure' }
    if ($env:DB_USER -ne 'wrong-console-value') { throw 'Existing variable not restored after failure' }
    if ($global:reconacTestConfigs -ne 2) { throw 'Real Compose validation not executed twice' }
    Write-Output 'PASS: variables removed, real Compose config succeeded, environment restored on success/failure.'
} finally {
    foreach ($name in $original.Keys) {
        if ($original[$name].Exists) {
            [Environment]::SetEnvironmentVariable($name, $original[$name].Value, 'Process')
        } else { Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue }
    }
    $temporaryBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $resolvedRoot = [IO.Path]::GetFullPath($root)
    if (-not $resolvedRoot.StartsWith($temporaryBase, [StringComparison]::OrdinalIgnoreCase) -or
        -not ([IO.Path]::GetFileName($resolvedRoot)).StartsWith('reconac-env-test-')) {
        throw 'Destino temporal inesperado; no se elimina.'
    }
    Remove-Item -LiteralPath $resolvedRoot -Recurse -Force

}
$global:LASTEXITCODE = 0
