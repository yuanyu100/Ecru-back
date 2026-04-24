param(
    [switch]$KeepMinio
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $repoRoot 'logs/dev'

function Get-DescendantProcessIds {
    param([int]$RootPid)

    $allProcesses = Get-CimInstance Win32_Process
    $pending = New-Object System.Collections.Generic.Queue[int]
    $found = New-Object System.Collections.Generic.List[int]
    $pending.Enqueue($RootPid)

    while ($pending.Count -gt 0) {
        $current = $pending.Dequeue()
        foreach ($child in $allProcesses | Where-Object { $_.ParentProcessId -eq $current }) {
            $found.Add([int]$child.ProcessId)
            $pending.Enqueue([int]$child.ProcessId)
        }
    }

    return $found
}

function Stop-ManagedProcess {
    param([string]$Name)

    $pidFile = Join-Path $logsDir "$Name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) {
        Write-Host "$Name is not managed by pid file."
        return
    }

    $rawPid = Get-Content -LiteralPath $pidFile -Raw -ErrorAction SilentlyContinue
    $pidValue = 0
    if (-not [int]::TryParse(($rawPid | Out-String).Trim(), [ref]$pidValue)) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        Write-Host "$Name pid file was invalid and has been removed."
        return
    }

    $targetProcess = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if (-not $targetProcess) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        Write-Host "$Name process was already stopped."
        return
    }

    $descendants = Get-DescendantProcessIds -RootPid $pidValue
    $stopList = @($descendants | Sort-Object -Descending)
    $stopList += $pidValue

    foreach ($id in $stopList) {
        $process = Get-Process -Id $id -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
        }
    }

    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    Write-Host "$Name stopped."
}

Stop-ManagedProcess -Name 'admin-app'
Stop-ManagedProcess -Name 'user-app'
Stop-ManagedProcess -Name 'backend'

if (-not $KeepMinio) {
    & (Join-Path $repoRoot 'scripts/stop-minio.ps1')
}
