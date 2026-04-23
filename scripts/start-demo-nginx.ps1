$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$nginxHome = 'D:/Tools/nginx-1.20.2'
$nginxExe = Join-Path $nginxHome 'nginx.exe'
$nginxPrefix = Join-Path $repoRoot 'deploy/nginx'
$nginxConfig = Join-Path $nginxPrefix 'nginx.conf'
$nginxLogs = Join-Path $nginxPrefix 'logs'
$nginxTemp = Join-Path $nginxPrefix 'temp'

if (-not (Test-Path $nginxExe)) {
    throw "Nginx not found: $nginxExe"
}

New-Item -ItemType Directory -Force -Path $nginxLogs | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $nginxTemp 'client_body_temp') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $nginxTemp 'proxy_temp') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $nginxTemp 'fastcgi_temp') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $nginxTemp 'uwsgi_temp') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $nginxTemp 'scgi_temp') | Out-Null

Push-Location (Join-Path $repoRoot 'frontend/admin-app')
npm run build
Pop-Location

Push-Location (Join-Path $repoRoot 'frontend/user-app')
npm run build
Pop-Location

& $nginxExe -p "$nginxPrefix/" -c $nginxConfig -t

$pidFile = Join-Path $nginxLogs 'nginx.pid'
if ((Test-Path $pidFile) -and [string]::IsNullOrWhiteSpace((Get-Content $pidFile -Raw))) {
    Remove-Item -LiteralPath $pidFile -Force
}

if (Test-Path $pidFile) {
    & $nginxExe -p "$nginxPrefix/" -c $nginxConfig -s reload
} else {
    Start-Process -FilePath $nginxExe -ArgumentList @('-p', "$nginxPrefix/", '-c', $nginxConfig) -WindowStyle Hidden | Out-Null
    Start-Sleep -Seconds 1
}

Write-Host 'Demo nginx is ready:'
Write-Host '  User app : http://127.0.0.1:8090/'
Write-Host '  Admin app: http://127.0.0.1:8090/admin/'
Write-Host '  API proxy: http://127.0.0.1:8090/api/v1/'
