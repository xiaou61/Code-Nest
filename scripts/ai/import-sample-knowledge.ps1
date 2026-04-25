param(
    [string]$ServiceUrl = "http://127.0.0.1:18080",
    [string]$ApiKey = "",
    [string]$FilePath = "",
    [switch]$Replace
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..\..")).Path

if ([string]::IsNullOrWhiteSpace($FilePath)) {
    $FilePath = Join-Path $repoRoot "llamaindex-service\data\sample-documents.json"
}

if (-not [System.IO.Path]::IsPathRooted($FilePath)) {
    $FilePath = Join-Path $repoRoot $FilePath
}

if (-not (Test-Path $FilePath)) {
    throw "未找到样例知识文件：$FilePath"
}

$documents = Get-Content -Path $FilePath -Raw -Encoding UTF8 | ConvertFrom-Json

$requestBody = @{
    replace = [bool]$Replace
    documents = $documents
} | ConvertTo-Json -Depth 20

$headers = @{
    "Content-Type" = "application/json"
}

$resolvedApiKey = $ApiKey
if ([string]::IsNullOrWhiteSpace($resolvedApiKey) -and -not [string]::IsNullOrWhiteSpace($env:LLAMAINDEX_SERVICE_API_KEY)) {
    $resolvedApiKey = $env:LLAMAINDEX_SERVICE_API_KEY
}

if (-not [string]::IsNullOrWhiteSpace($resolvedApiKey)) {
    $headers["Authorization"] = "Bearer " + $resolvedApiKey.Trim()
}

$endpoint = $ServiceUrl.TrimEnd("/") + "/api/v1/admin/documents/import"

Write-Host "[AI] 开始导入样例知识..." -ForegroundColor Cyan
Write-Host "      Endpoint : $endpoint"
Write-Host "      File     : $FilePath"
Write-Host "      Replace  : $([bool]$Replace)"

$response = Invoke-RestMethod -Method Post -Uri $endpoint -Headers $headers -Body $requestBody

Write-Host "[AI] 导入完成 importedCount=$($response.importedCount) totalCount=$($response.totalCount)" -ForegroundColor Green
