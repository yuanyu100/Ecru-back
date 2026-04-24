$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $repoRoot 'logs/dev'

$services = @(
    @{ Name = 'backend'; Port = 8081; Url = 'http://127.0.0.1:8081/api/v1'; Stdout = 'backend.out.log'; Stderr = 'backend.err.log' },
    @{ Name = 'user-app'; Port = 5173; Url = 'http://127.0.0.1:5173/'; Stdout = 'user-app.out.log'; Stderr = 'user-app.err.log' },
    @{ Name = 'admin-app'; Port = 5174; Url = 'http://127.0.0.1:5174/admin/'; Stdout = 'admin-app.out.log'; Stderr = 'admin-app.err.log' }
)

function Test-PortListening {
    param(
        [string]$HostName,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(700)
        if ($connected -and $client.Connected) {
            $client.EndConnect($asyncResult) | Out-Null
            return $true
        }
        return $false
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

foreach ($service in $services) {
    $pidFile = Join-Path $logsDir ($service.Name + '.pid')
    $stdoutFile = Join-Path $logsDir $service.Stdout
    $stderrFile = Join-Path $logsDir $service.Stderr

    $pidValue = $null
    $process = $null

    if (Test-Path -LiteralPath $pidFile) {
        $rawPid = Get-Content -LiteralPath $pidFile -Raw -ErrorAction SilentlyContinue
        $parsedPid = 0
        if ([int]::TryParse(($rawPid | Out-String).Trim(), [ref]$parsedPid)) {
            $pidValue = $parsedPid
            $process = Get-Process -Id $parsedPid -ErrorAction SilentlyContinue
        }
    }

    $portListening = Test-PortListening -HostName '127.0.0.1' -Port $service.Port
    $status = if ($process) { 'RUNNING' } elseif ($portListening) { 'EXTERNAL' } else { 'STOPPED' }

    Write-Host ''
    Write-Host "$($service.Name): $status"
    Write-Host "  URL    : $($service.Url)"
    Write-Host "  Port   : $($service.Port) -> $(if ($portListening) { 'listening' } else { 'not listening' })"
    Write-Host "  PID    : $(if ($pidValue) { $pidValue } else { '-' })"
    Write-Host "  stdout : $stdoutFile"
    Write-Host "  stderr : $stderrFile"
}

Write-Host ''
Write-Host "MinIO API    : $(if (Test-PortListening -HostName '127.0.0.1' -Port 9000) { 'RUNNING' } else { 'STOPPED' })"
Write-Host "MinIO Console: $(if (Test-PortListening -HostName '127.0.0.1' -Port 9001) { 'RUNNING' } else { 'STOPPED' })"
