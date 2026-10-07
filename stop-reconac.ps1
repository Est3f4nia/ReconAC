$ErrorActionPreference = 'Stop'
try {
    & "$PSScriptRoot/scripts/reconac.ps1" -Action stop
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
