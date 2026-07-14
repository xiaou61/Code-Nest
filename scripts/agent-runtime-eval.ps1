param(
    [switch]$LiveAi,
    [switch]$LiveWrite,
    [string]$AiBaseUrl = $env:XIAOU_AI_BASE_URL,
    [string]$AiApiKey = $env:XIAOU_AI_API_KEY,
    [string]$AiChatModel = $(if ($env:XIAOU_AI_CHAT_MODEL) { $env:XIAOU_AI_CHAT_MODEL } else { "gpt-5.5" }),
    [string]$MysqlUrl = $env:AGENT_TEST_MYSQL_URL,
    [string]$MysqlUsername = $env:AGENT_TEST_MYSQL_USERNAME,
    [string]$MysqlPassword = $env:AGENT_TEST_MYSQL_PASSWORD
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$backendRuntimeTests = @(
    "AgentChatControllerTest",
    "AgentChatOrchestratorTest",
    "AgentOperatorTest",
    "AgentPolicyEngineTest",
    "AgentToolRegistryTest",
    "AgentSessionContextStoreTest",
    "InMemoryAgentSessionContextRepositoryTest",
    "RedisAgentSessionContextRepositoryTest",
    "DbAgentSessionContextRepositoryTest",
    "AgentToolMetricsRecorderTest",
    "AgentToolDefinitionBuilderTest",
    "AbstractReadonlyAgentToolTest",
    "AgentToolDefinitionContractTest",
    "AgentToolAccessDefinitionTest",
    "AgentPermissionSeedContractTest",
    "AgentToolCatalogAgentToolTest",
    "AgentToolDetailAgentToolTest",
    "AgentToolSearchAgentToolTest",
    "AgentToolDefinitionValidateAgentToolTest",
    "AgentToolAccessCheckAgentToolTest",
    "AgentPolicyDryRunAgentToolTest",
    "AgentRuntimeStatusAgentToolTest",
    "AgentRuntimeReadinessAgentToolTest",
    "AgentRuntimeObservabilityAgentToolTest",
    "AgentSessionContextAgentToolTest",
    "AgentOperatorSelfAgentToolTest",
    "AgentPlannerDiagnosticsAgentToolTest",
    "AgentPlannerDryRunAgentToolTest",
    "AgentRequestDryRunAgentToolTest",
    "AgentToolMetricsSummaryAgentToolTest",
    "AgentToolMetricsHealthAgentToolTest",
    "AgentToolMetricsAlertsAgentToolTest",
    "AgentAuditListAgentToolTest",
    "AgentAuditDetailAgentToolTest",
    "AgentRecoveryExplainAgentToolTest",
    "AgentRecoveryDryRunAgentToolTest",
    "OperationLogListAgentToolTest",
    "ChatUserBanAgentToolTest",
    "LotteryRealtimeMonitorAgentToolTest",
    "LlmAgentPlanResolverTest",
    "AgentRuntimeLiveAiSmokeTest",
    "AgentRuntimeLiveWriteAcceptanceTest",
    "AgentRuntimeAllToolsUnifiedEntryTest",
    "AdminAgentPlannerRegressionTest",
    "SysAgentAuditServiceImplTest"
)

$aiStructuredTests = @(
    "AiPromptSpecTest",
    "AiStructuredOutputSpecTest",
    "AiStructuredOutputValidatorTest",
    "AiStructuredOutputSchemaExporterTest",
    "AiRagRetrievalProfileTest"
)

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "[agent-runtime-eval] $Name" -ForegroundColor Cyan
    & $Command
}

function Invoke-Native {
    param(
        [string]$Command,
        [string[]]$Arguments = @()
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: $Command $($Arguments -join ' ')"
    }
}

function Invoke-MavenTests {
    param(
        [string]$Module,
        [string[]]$Tests
    )

    $args = @(
        "-pl", $Module,
        "-am",
        "-Dtest=$($Tests -join ',')",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "test",
        "-q"
    )
    Invoke-Native -Command "mvn" -Arguments $args
}

function Test-TextFileHasNul {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    return $bytes -contains 0
}

function Invoke-NulScan {
    $excludedSegments = @(
        "\.git\",
        "\.codegraph\",
        "\.codex-mihomo-geo\",
        "\node_modules\",
        "\target\",
        "\dist\",
        "\build\",
        "\__pycache__\"
    )
    $textExtensions = @(
        ".java", ".xml", ".json", ".md", ".yml", ".yaml", ".js", ".ts", ".vue",
        ".ps1", ".sql", ".properties", ".txt", ".css", ".html", ".csv"
    )
    $violations = New-Object System.Collections.Generic.List[string]

    Get-ChildItem -Path $repoRoot -Recurse -File | ForEach-Object {
        $fullName = $_.FullName
        $excluded = $false
        foreach ($segment in $excludedSegments) {
            if ($fullName.Contains($segment)) {
                $excluded = $true
                break
            }
        }

        $extension = $_.Extension.ToLowerInvariant()
        $isTextLike = $textExtensions.Contains($extension) -or $_.Name -eq "pom-xml-flattened"
        if (-not $excluded -and $isTextLike -and (Test-TextFileHasNul -Path $fullName)) {
            $violations.Add($fullName)
        }
    }

    if ($violations.Count -gt 0) {
        $violations | ForEach-Object { Write-Error "NUL byte detected: $_" }
        throw "NUL scan failed"
    }

    Write-Host "[agent-runtime-eval] NUL scan passed" -ForegroundColor Green
}

$previousLiveAi = $env:AGENT_LIVE_AI_TEST
$previousBaseUrl = $env:XIAOU_AI_BASE_URL
$previousApiKey = $env:XIAOU_AI_API_KEY
$previousChatModel = $env:XIAOU_AI_CHAT_MODEL
$previousLiveWrite = $env:AGENT_LIVE_WRITE_TEST
$previousMysqlUrl = $env:AGENT_TEST_MYSQL_URL
$previousMysqlUsername = $env:AGENT_TEST_MYSQL_USERNAME
$previousMysqlPassword = $env:AGENT_TEST_MYSQL_PASSWORD

try {
    if ($LiveWrite) {
        $LiveAi = $true
    }
    if ($LiveAi) {
        if ([string]::IsNullOrWhiteSpace($AiBaseUrl)) {
            throw "Live AI acceptance requested but XIAOU_AI_BASE_URL / -AiBaseUrl is missing"
        }
        if ([string]::IsNullOrWhiteSpace($AiApiKey)) {
            throw "Live AI acceptance requested but XIAOU_AI_API_KEY / -AiApiKey is missing"
        }
        $env:AGENT_LIVE_AI_TEST = "true"
        $env:XIAOU_AI_BASE_URL = $AiBaseUrl
        $env:XIAOU_AI_API_KEY = $AiApiKey
        $env:XIAOU_AI_CHAT_MODEL = $AiChatModel
        Write-Host "[agent-runtime-eval] Live AI acceptance enabled: baseUrl=$AiBaseUrl model=$AiChatModel" -ForegroundColor Yellow
    } else {
        $env:AGENT_LIVE_AI_TEST = "false"
        Write-Host "[agent-runtime-eval] Live AI acceptance disabled; pass -LiveAi to enable it" -ForegroundColor Yellow
    }

    if ($LiveWrite) {
        if ([string]::IsNullOrWhiteSpace($MysqlUrl)) {
            throw "Live write acceptance requested but AGENT_TEST_MYSQL_URL / -MysqlUrl is missing"
        }
        if ([string]::IsNullOrWhiteSpace($MysqlUsername)) {
            throw "Live write acceptance requested but AGENT_TEST_MYSQL_USERNAME / -MysqlUsername is missing"
        }
        if ([string]::IsNullOrWhiteSpace($MysqlPassword)) {
            throw "Live write acceptance requested but AGENT_TEST_MYSQL_PASSWORD / -MysqlPassword is missing"
        }
        $env:AGENT_LIVE_WRITE_TEST = "true"
        $env:AGENT_TEST_MYSQL_URL = $MysqlUrl
        $env:AGENT_TEST_MYSQL_USERNAME = $MysqlUsername
        $env:AGENT_TEST_MYSQL_PASSWORD = $MysqlPassword
        Write-Host "[agent-runtime-eval] Live write acceptance enabled with an isolated temporary MySQL database" -ForegroundColor Yellow
    } else {
        $env:AGENT_LIVE_WRITE_TEST = "false"
        Write-Host "[agent-runtime-eval] Live write acceptance disabled; pass -LiveWrite to enable it" -ForegroundColor Yellow
    }

    Invoke-Step "backend unified runtime regression" {
        Invoke-MavenTests -Module "xiaou-system" -Tests $backendRuntimeTests
    }

    Invoke-Step "AI prompt and structured output regression" {
        Invoke-MavenTests -Module "xiaou-ai" -Tests $aiStructuredTests
    }

    Invoke-Step "frontend thin-boundary regression" {
        Invoke-Native -Command "node" -Arguments @(
            "--test",
            "vue3-admin-front/tests/admin-api-contract.test.js",
            "vue3-admin-front/tests/admin-agent-chat-ui.test.js"
        )
    }

    Invoke-Step "git diff whitespace check" {
        Invoke-Native -Command "git" -Arguments @("diff", "--check")
    }

    Invoke-Step "text NUL-byte scan" {
        Invoke-NulScan
    }

    Write-Host ""
    Write-Host "[agent-runtime-eval] all checks passed" -ForegroundColor Green
} finally {
    $env:AGENT_LIVE_AI_TEST = $previousLiveAi
    $env:XIAOU_AI_BASE_URL = $previousBaseUrl
    $env:XIAOU_AI_API_KEY = $previousApiKey
    $env:XIAOU_AI_CHAT_MODEL = $previousChatModel
    $env:AGENT_LIVE_WRITE_TEST = $previousLiveWrite
    $env:AGENT_TEST_MYSQL_URL = $previousMysqlUrl
    $env:AGENT_TEST_MYSQL_USERNAME = $previousMysqlUsername
    $env:AGENT_TEST_MYSQL_PASSWORD = $previousMysqlPassword
}
