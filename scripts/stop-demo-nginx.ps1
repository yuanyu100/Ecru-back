$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$nginxExe = 'D:/Tools/nginx-1.20.2/nginx.exe'
$nginxPrefix = Join-Path $repoRoot 'deploy/nginx'
$pidFile = Join-Path $nginxPrefix 'logs/nginx.pid'

if (-not (Test-Path $nginxExe)) {
    throw "Nginx not found: $nginxExe"
}

if (Test-Path $pidFile) {
    & $nginxExe -p "$nginxPrefix/" -c (Join-Path $nginxPrefix 'nginx.conf') -s quit
    Write-Host 'Demo nginx stopped.'
} else {
    Write-Host 'Demo nginx is not running.'
}
