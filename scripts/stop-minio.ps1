$ErrorActionPreference = 'Stop'

$targets = Get-Process -Name 'minio' -ErrorAction SilentlyContinue

if ($targets) {
    $targets | ForEach-Object { Stop-Process -Id $_.Id -Force }
    Write-Host 'MinIO stopped.'
} else {
    Write-Host 'MinIO is not running.'
}
