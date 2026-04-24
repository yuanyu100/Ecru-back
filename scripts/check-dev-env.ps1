$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Category,
        [string]$Name,
        [string]$Status,
        [string]$Detail
    )

    $results.Add([PSCustomObject]@{
            Category = $Category
            Name     = $Name
            Status   = $Status
            Detail   = $Detail
        })
}

function Test-CommandPath {
    param(
        [string]$Category,
        [string]$CommandName,
        [switch]$Required
    )

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($command) {
        Add-Result $Category $CommandName 'OK' $command.Source
        return
    }

    $status = if ($Required) { 'FAIL' } else { 'WARN' }
    Add-Result $Category $CommandName $status 'Not found in PATH'
}

function Test-FilePath {
    param(
        [string]$Category,
        [string]$Name,
        [string]$Path,
        [switch]$Required
    )

    if (Test-Path -LiteralPath $Path) {
        Add-Result $Category $Name 'OK' $Path
        return
    }

    $status = if ($Required) { 'FAIL' } else { 'WARN' }
    Add-Result $Category $Name $status "Missing: $Path"
}

function Test-TcpPort {
    param(
        [string]$Category,
        [string]$Name,
        [string]$TargetHost,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($TargetHost, $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(800)
        if ($connected -and $client.Connected) {
            $client.EndConnect($asyncResult) | Out-Null
            Add-Result $Category $Name 'OK' "$TargetHost`:$Port is reachable"
        } else {
            Add-Result $Category $Name 'WARN' "$TargetHost`:$Port is not listening"
        }
    } catch {
        Add-Result $Category $Name 'WARN' "$TargetHost`:$Port is not reachable"
    } finally {
        $client.Close()
    }
}

function Test-EnvVar {
    param(
        [string]$Name
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        Add-Result 'ENV' $Name 'WARN' 'Not set'
        return
    }

    $masked = if ($value.Length -le 6) { '***' } else { $value.Substring(0, 3) + '***' + $value.Substring($value.Length - 3) }
    Add-Result 'ENV' $Name 'OK' "Set: $masked"
}

Test-FilePath 'FILE' 'backend application.yml' (Join-Path $repoRoot 'backend/ecru-web/src/main/resources/application.yml') -Required
Test-FilePath 'FILE' 'start-minio.ps1' (Join-Path $repoRoot 'scripts/start-minio.ps1') -Required
Test-FilePath 'FILE' 'stop-minio.ps1' (Join-Path $repoRoot 'scripts/stop-minio.ps1') -Required
Test-FilePath 'FILE' 'start-demo-nginx.ps1' (Join-Path $repoRoot 'scripts/start-demo-nginx.ps1')
Test-FilePath 'FILE' 'stop-demo-nginx.ps1' (Join-Path $repoRoot 'scripts/stop-demo-nginx.ps1')
Test-FilePath 'FILE' 'prepare-demo-data.ps1' (Join-Path $repoRoot 'backend/manual-tests/prepare-demo-data.ps1')

Test-CommandPath 'CMD' 'java.exe' -Required
Test-CommandPath 'CMD' 'node.exe' -Required
Test-CommandPath 'CMD' 'npm.cmd' -Required
Test-CommandPath 'CMD' 'mvn.cmd' -Required
Test-CommandPath 'CMD' 'mysql.exe' -Required
Test-CommandPath 'CMD' 'psql.exe' -Required

Test-FilePath 'DEPENDENCY' 'MinIO executable' 'D:/Tools/minIO/bin/minio.exe'
Test-FilePath 'DEPENDENCY' 'Nginx executable' 'D:/Tools/nginx-1.20.2/nginx.exe'

Test-TcpPort 'PORT' 'MySQL 3306' '127.0.0.1' 3306
Test-TcpPort 'PORT' 'PostgreSQL 5432' '127.0.0.1' 5432
Test-TcpPort 'PORT' 'MinIO API 9000' '127.0.0.1' 9000
Test-TcpPort 'PORT' 'MinIO Console 9001' '127.0.0.1' 9001
Test-TcpPort 'PORT' 'Backend API 8081' '127.0.0.1' 8081
Test-TcpPort 'PORT' 'Demo Nginx 8090' '127.0.0.1' 8090

Test-EnvVar 'AI_API_KEY'
Test-EnvVar 'SILICONFLOW_API_KEY'
Test-EnvVar 'MCP_WEATHER_API_KEY'

$statusOrder = @{
    FAIL = 0
    WARN = 1
    OK   = 2
}

$sorted = $results | Sort-Object Category, @{ Expression = { $statusOrder[$_.Status] } }, Name

Write-Host ''
Write-Host 'Ecru local environment check'
Write-Host '============================'
Write-Host ''

foreach ($item in $sorted) {
    $prefix = switch ($item.Status) {
        'OK' { '[OK]  ' }
        'WARN' { '[WARN]' }
        'FAIL' { '[FAIL]' }
        default { '[INFO]' }
    }
    Write-Host ("{0} [{1}] {2} - {3}" -f $prefix, $item.Category, $item.Name, $item.Detail)
}

$failCount = ($results | Where-Object { $_.Status -eq 'FAIL' }).Count
$warnCount = ($results | Where-Object { $_.Status -eq 'WARN' }).Count
$okCount = ($results | Where-Object { $_.Status -eq 'OK' }).Count

Write-Host ''
Write-Host ("Summary: {0} OK, {1} WARN, {2} FAIL" -f $okCount, $warnCount, $failCount)

if ($warnCount -gt 0) {
    Write-Host ''
    Write-Host 'Recommended next actions:'
    Write-Host '  1. Start missing services with scripts in ./scripts'
    Write-Host '  2. Start backend/frontend and rerun this check'
    Write-Host '  3. Configure missing AI env vars before AI demos'
}

if ($failCount -gt 0) {
    exit 1
}
