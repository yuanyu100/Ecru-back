param(
    [switch]$SkipCheck,
    [switch]$SkipMinio,
    [switch]$SkipBackend,
    [switch]$SkipUserApp,
    [switch]$SkipAdminApp
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $repoRoot 'logs/dev'
$powerShellExe = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'

New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

function Escape-SingleQuotedText {
    param([string]$Value)
    return ($Value -replace "'", "''")
}

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

function Get-RunningProcessFromPidFile {
    param([string]$PidFile)

    if (-not (Test-Path -LiteralPath $PidFile)) {
        return $null
    }

    $rawPid = Get-Content -LiteralPath $PidFile -Raw -ErrorAction SilentlyContinue
    if ([string]::IsNullOrWhiteSpace($rawPid)) {
        return $null
    }

    $pidValue = 0
    if (-not [int]::TryParse($rawPid.Trim(), [ref]$pidValue)) {
        return $null
    }

    return Get-Process -Id $pidValue -ErrorAction SilentlyContinue
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string]$Executable,
        [string[]]$Arguments,
        [int]$Port
    )

    $pidFile = Join-Path $logsDir "$Name.pid"
    $stdoutFile = Join-Path $logsDir "$Name.out.log"
    $stderrFile = Join-Path $logsDir "$Name.err.log"

    $existing = Get-RunningProcessFromPidFile -PidFile $pidFile
    if ($existing) {
        Write-Host "$Name is already running. PID=$($existing.Id)"
        return
    }

    if (Test-PortListening -HostName '127.0.0.1' -Port $Port) {
        Write-Host "$Name port $Port is already in use. Skipping managed start."
        return
    }

    $escapedWorkdir = Escape-SingleQuotedText $WorkingDirectory
    $escapedExecutable = Escape-SingleQuotedText $Executable
    $escapedArgs = @()
    foreach ($arg in $Arguments) {
        $escapedArgs += "'" + (Escape-SingleQuotedText $arg) + "'"
    }
    $command = "& { Set-Location '$escapedWorkdir'; & '$escapedExecutable' $($escapedArgs -join ' ') }"

    $process = Start-Process `
        -FilePath $powerShellExe `
        -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $command) `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdoutFile `
        -RedirectStandardError $stderrFile `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ascii
    Write-Host "$Name started. PID=$($process.Id) Port=$Port"
}

if (-not $SkipCheck) {
    & (Join-Path $repoRoot 'scripts/check-dev-env.ps1')
}

if (-not $SkipMinio) {
    & (Join-Path $repoRoot 'scripts/start-minio.ps1')
}

if (-not $SkipBackend) {
    $mavenCommand = (Get-Command 'mvn.cmd' -ErrorAction Stop).Source
    Start-ManagedProcess `
        -Name 'backend' `
        -WorkingDirectory (Join-Path $repoRoot 'backend') `
        -Executable $mavenCommand `
        -Arguments @('spring-boot:run', '-pl', 'ecru-web') `
        -Port 8081
}

if (-not $SkipUserApp) {
    $npmCommand = (Get-Command 'npm.cmd' -ErrorAction Stop).Source
    Start-ManagedProcess `
        -Name 'user-app' `
        -WorkingDirectory (Join-Path $repoRoot 'frontend/user-app') `
        -Executable $npmCommand `
        -Arguments @('run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173') `
        -Port 5173
}

if (-not $SkipAdminApp) {
    $npmCommand = (Get-Command 'npm.cmd' -ErrorAction Stop).Source
    Start-ManagedProcess `
        -Name 'admin-app' `
        -WorkingDirectory (Join-Path $repoRoot 'frontend/admin-app') `
        -Executable $npmCommand `
        -Arguments @('run', 'dev', '--', '--host', '127.0.0.1', '--port', '5174') `
        -Port 5174
}

Write-Host ''
Write-Host 'Managed dev services summary:'
Write-Host '  Backend : http://127.0.0.1:8081/api/v1'
Write-Host '  User app: http://127.0.0.1:5173/'
Write-Host '  Admin   : http://127.0.0.1:5174/admin/'
Write-Host '  Logs dir: logs/dev/'
Write-Host ''
Write-Host 'Check status with: .\scripts\status-local-dev.ps1'
Write-Host 'Stop all with:     .\scripts\stop-local-dev.ps1'
