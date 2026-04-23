$ErrorActionPreference = 'Stop'

$targets = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq 'minio.exe' -and $_.CommandLine -like '*D:\Tools\minIO\data*'
}

if ($targets) {
    $targets | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
    Write-Host 'MinIO stopped.'
} else {
    Write-Host 'MinIO is not running.'
}
