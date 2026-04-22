param(
    [string]$Python = "python",
    [string]$ApiKey = "",
    [string]$BindHost = "0.0.0.0",
    [int]$Port = 18080,
    [string]$DataFile = "",
    [switch]$InstallDependencies,
    [switch]$DisableVenv
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

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..")).Path
$serviceDir = Join-Path $repoRoot "llamaindex-service"
$venvDir = Join-Path $serviceDir ".venv"
$venvPython = Join-Path $venvDir "Scripts\python.exe"
$requirementsFile = Join-Path $serviceDir "requirements.txt"

if ([string]::IsNullOrWhiteSpace($DataFile)) {
    $DataFile = Join-Path $serviceDir "data\knowledge-base.json"
}

$runtimePython = $Python
$venvCreated = $false

if (-not $DisableVenv) {
    if (-not (Test-Path $venvPython)) {
        Write-Host "[AI] 未检测到 Python 虚拟环境，开始创建 llamaindex-service/.venv ..." -ForegroundColor Cyan
        & $Python -m venv $venvDir
        $venvCreated = $true
    }
    $runtimePython = $venvPython
}

if ($venvCreated -or $InstallDependencies) {
    Write-Host "[AI] 安装或更新 Python 依赖..." -ForegroundColor Cyan
    & $runtimePython -m pip install --upgrade pip
    & $runtimePython -m pip install -r $requirementsFile
}

if ([System.IO.Path]::IsPathRooted($runtimePython) -or $runtimePython.Contains("\") -or $runtimePython.Contains("/")) {
    if (-not (Test-Path $runtimePython)) {
        throw "未找到可用的 Python 解释器：$runtimePython"
    }
} elseif (-not (Get-Command $runtimePython -ErrorAction SilentlyContinue)) {
    throw "未找到可用的 Python 命令：$runtimePython"
}

$resolvedDataFile = $DataFile
if (-not [System.IO.Path]::IsPathRooted($resolvedDataFile)) {
    $resolvedDataFile = Join-Path $repoRoot $resolvedDataFile
}

$dataDirectory = Split-Path -Parent $resolvedDataFile
if (-not (Test-Path $dataDirectory)) {
    New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null
}

if (-not [string]::IsNullOrWhiteSpace($ApiKey)) {
    $env:LLAMAINDEX_SERVICE_API_KEY = $ApiKey.Trim()
}

$env:LLAMAINDEX_DATA_FILE = $resolvedDataFile
$env:PYTHONPATH = $serviceDir

Write-Host "[AI] LlamaIndex 检索服务准备完成" -ForegroundColor Green
Write-Host "      Python      : $runtimePython"
Write-Host "      Host/Port   : $BindHost`:$Port"
Write-Host "      Data File   : $resolvedDataFile"
Write-Host "      API Key     : $(Get-MaskedValue $env:LLAMAINDEX_SERVICE_API_KEY)"
Write-Host "      Health URL  : http://127.0.0.1:$Port/health"
Write-Host ""
Write-Host "[AI] 如果 Java 侧要联调，请确认 xiaou.ai.rag.endpoint 指向 http://127.0.0.1:$Port" -ForegroundColor Yellow

Push-Location $serviceDir
try {
    & $runtimePython -m uvicorn app.main:app --host $BindHost --port $Port --reload
} finally {
    Pop-Location
}
