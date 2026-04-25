param(
    [string]$Maven = "mvn",
    [string]$Python = "python",
    [string]$SpringProfile = "dev",
    [string]$RagApiKey = "",
    [int]$RagPort = 18080,
    [string]$RagDataFile = "",
    [switch]$InstallRagDependencies,
    [switch]$DisableRagVenv,
    [switch]$ImportSampleKnowledge,
    [switch]$SkipRagSidecar,
    [switch]$SkipJava
)

$ErrorActionPreference = "Stop"

function Get-MaskedValue {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return "未配置"
    }

    if ($Value.Length -le 10) {
        return ($Value.Substring(0, 2) + "******")
    }

    return ($Value.Substring(0, 4) + "******" + $Value.Substring($Value.Length - 2))
}

function Wait-RagHealth {
    param(
        [string]$HealthUrl,
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 3
            if ($response.status -eq "ok") {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..")).Path
$ragScript = Join-Path $scriptDir "start-llamaindex-service.ps1"
$importScript = Join-Path $scriptDir "import-sample-knowledge.ps1"
$ragServiceUrl = "http://127.0.0.1:$RagPort"
$ragHealthUrl = "$ragServiceUrl/health"

if (-not $SkipRagSidecar) {
    $ragArguments = @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-File", $ragScript,
        "-Python", $Python,
        "-Port", $RagPort
    )

    if (-not [string]::IsNullOrWhiteSpace($RagApiKey)) {
        $ragArguments += @("-ApiKey", $RagApiKey.Trim())
    }
    if (-not [string]::IsNullOrWhiteSpace($RagDataFile)) {
        $ragArguments += @("-DataFile", $RagDataFile)
    }
    if ($InstallRagDependencies) {
        $ragArguments += "-InstallDependencies"
    }
    if ($DisableRagVenv) {
        $ragArguments += "-DisableVenv"
    }

    Write-Host "[AI] 正在新开 PowerShell 窗口启动 RAG sidecar..." -ForegroundColor Cyan
    Start-Process -FilePath "powershell" -ArgumentList $ragArguments | Out-Null

    Write-Host "[AI] 等待 RAG sidecar 健康检查通过：$ragHealthUrl" -ForegroundColor Cyan
    if (-not (Wait-RagHealth -HealthUrl $ragHealthUrl)) {
        throw "RAG sidecar 启动超时，请检查新开的 PowerShell 窗口日志。"
    }

    Write-Host "[AI] RAG sidecar 已可达" -ForegroundColor Green
}

if ($ImportSampleKnowledge) {
    Write-Host "[AI] 开始导入样例知识..." -ForegroundColor Cyan
    $importArguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $importScript,
        "-ServiceUrl", $ragServiceUrl,
        "-Replace"
    )
    if (-not [string]::IsNullOrWhiteSpace($RagApiKey)) {
        $importArguments += @("-ApiKey", $RagApiKey.Trim())
    }
    & "powershell" @importArguments
}

$env:XIAOU_AI_RAG_ENABLED = "true"
$env:XIAOU_AI_RAG_ENDPOINT = $ragServiceUrl
if (-not [string]::IsNullOrWhiteSpace($RagApiKey)) {
    $env:XIAOU_AI_RAG_API_KEY = $RagApiKey.Trim()
}

Write-Host "[AI] 当前会话已注入 Java 侧 RAG 环境变量" -ForegroundColor Green
Write-Host "      XIAOU_AI_RAG_ENABLED : $env:XIAOU_AI_RAG_ENABLED"
Write-Host "      XIAOU_AI_RAG_ENDPOINT: $env:XIAOU_AI_RAG_ENDPOINT"
Write-Host "      XIAOU_AI_RAG_API_KEY : $(Get-MaskedValue $env:XIAOU_AI_RAG_API_KEY)"

if ($SkipJava) {
    Write-Host "[AI] 已跳过 Java 启动，可直接在当前终端执行 Maven 命令。" -ForegroundColor Yellow
    exit 0
}

Write-Host "[AI] 开始启动 Java 主服务（profile=$SpringProfile）..." -ForegroundColor Cyan
Push-Location $repoRoot
try {
    & $Maven "-Dspring-boot.run.profiles=$SpringProfile" -pl xiaou-application -am spring-boot:run
} finally {
    Pop-Location
}
