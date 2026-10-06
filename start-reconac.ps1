$ErrorActionPreference = 'Stop'
try {
    & "$PSScriptRoot/scripts/reconac.ps1" -Action start
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
