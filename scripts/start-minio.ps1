$ErrorActionPreference = 'Stop'

$minioHome = 'D:/Tools/minIO'
$minioExe = Join-Path $minioHome 'bin/minio.exe'
$minioData = Join-Path $minioHome 'data'

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

if (-not (Test-Path $minioExe)) {
    throw "MinIO not found: $minioExe"
}

if (-not (Test-PortListening -HostName '127.0.0.1' -Port 9000)) {
    Start-Process -FilePath $minioExe -ArgumentList @('server', $minioData, '--address', ':9000', '--console-address', ':9001') -WorkingDirectory (Join-Path $minioHome 'bin') | Out-Null
    Start-Sleep -Seconds 2
}

Write-Host 'MinIO ready:'
Write-Host '  API    : http://127.0.0.1:9000'
Write-Host '  Console: http://127.0.0.1:9001'
Write-Host '  User   : minioadmin'
Write-Host '  Pass   : minioadmin'
