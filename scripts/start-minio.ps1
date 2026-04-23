$ErrorActionPreference = 'Stop'

$minioHome = 'D:/Tools/minIO'
$minioExe = Join-Path $minioHome 'bin/minio.exe'
$minioData = Join-Path $minioHome 'data'

if (-not (Test-Path $minioExe)) {
    throw "MinIO not found: $minioExe"
}

$running = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq 'minio.exe' -and $_.CommandLine -like '*D:\Tools\minIO\data*'
}

if (-not $running) {
    Start-Process -FilePath $minioExe -ArgumentList @('server', $minioData, '--address', ':9000', '--console-address', ':9001') -WorkingDirectory (Join-Path $minioHome 'bin') | Out-Null
    Start-Sleep -Seconds 2
}

Write-Host 'MinIO ready:'
Write-Host '  API    : http://127.0.0.1:9000'
Write-Host '  Console: http://127.0.0.1:9001'
Write-Host '  User   : minioadmin'
Write-Host '  Pass   : minioadmin'
