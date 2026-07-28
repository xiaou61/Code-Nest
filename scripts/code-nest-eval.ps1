param(
    [ValidateSet("smoke", "hygiene", "agent", "ai", "sre", "rag", "frontend", "backend", "release", "all")]
    [string]$Tier = "smoke",
    [switch]$LiveAi,
    [switch]$LiveWrite,
    [switch]$InstallPythonDeps,
    [string]$PythonCommand = $(if ($env:CODE_NEST_PYTHON) { $env:CODE_NEST_PYTHON } else { "python" }),
    [string]$BashCommand = $env:CODE_NEST_BASH,
    [string]$AiBaseUrl = $env:XIAOU_AI_BASE_URL,
    [string]$AiApiKey = $env:XIAOU_AI_API_KEY,
    [string]$AiChatModel = $(if ($env:XIAOU_AI_CHAT_MODEL) { $env:XIAOU_AI_CHAT_MODEL } else { "gpt-5.5" }),
    [string]$MysqlUrl = $env:AGENT_TEST_MYSQL_URL,
    [string]$MysqlUsername = $env:AGENT_TEST_MYSQL_USERNAME,
    [string]$MysqlPassword = $env:AGENT_TEST_MYSQL_PASSWORD
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.UTF8Encoding]::new()
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$backendModulesWithTests = @(
    "xiaou-system",
    "xiaou-ai",
    "xiaou-oj",
    "xiaou-learning-asset",
    "xiaou-mock-interview",
    "xiaou-community",
    "xiaou-user",
    "xiaou-points",
    "xiaou-filestorage",
    "xiaou-sensitive",
    "xiaou-moyu",
    "xiaou-sql-optimizer"
)

$aiRegressionTests = @(
    "AiSceneRegressionEvalTest",
    "InterviewGraphTest",
    "JobBattleGraphTest",
    "SqlOptimizeGraphTest",
    "AiSqlOptimizeServiceImplTest",
    "AiPromptSpecTest",
    "AiStructuredOutputValidatorTest",
    "LlamaIndexClientTest"
)

$sreRegressionTests = @(
    "SreInvestigationPromptContractTest",
    "SreInvestigationPlannerImplTest",
    "SreReadOnlyInvestigationToolServiceImplTest",
    "SreMetricsRecorderTest",
    "SreOperationalMetricsPublisherTest",
    "SreRcaReportTest",
    "SreRcaAnalyzerImplTest",
    "SreIncidentRcaServiceImplTest",
    "SreRcaEvaluationCaseServiceImplTest",
    "SreRcaEvaluationSuiteServiceImplTest",
    "SreRcaEvaluationRunServiceImplTest",
    "SreRcaEvaluationQueueServiceImplTest",
    "SreRcaEvaluationScorerTest",
    "SreRcaEvaluationGateEvaluatorTest",
    "SreRcaEvaluationSuiteGateTest",
    "SreRcaEvaluationServiceImplTest",
    "SreRcaEvaluationAdminControllerTest",
    "SreRcaEvaluationWorkerTest"
)

$frontendTests = @(
    "vue3-admin-front/tests/admin-api-contract.test.js",
    "vue3-admin-front/tests/admin-agent-chat-ui.test.js",
    "vue3-admin-front/tests/design-system-demo-routes.test.js",
    "vue3-admin-front/tests/no-debug-console.test.js",
    "vue3-admin-front/tests/request-options.test.js",
    "vue3-admin-front/tests/sidebar-routes.test.js",
    "vue3-admin-front/tests/sre-workbench-contract.test.js",
    "vue3-user-front/tests/captcha-api-contract.test.js",
    "vue3-user-front/tests/career-loop-adapter.test.js",
    "vue3-user-front/tests/design-system-demo-routes.test.js",
    "vue3-user-front/tests/home-data-adapter.test.js",
    "vue3-user-front/tests/interview-navigation.test.js",
    "vue3-user-front/tests/moyu-api-contract.test.js",
    "vue3-user-front/tests/navigation-routes.test.js",
    "vue3-user-front/tests/no-debug-console.test.js",
    "vue3-user-front/tests/oj-contest-adapter.test.js",
    "vue3-user-front/tests/request-options.test.js"
)

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "[code-nest-eval] $Name" -ForegroundColor Cyan
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

function Invoke-Maven {
    param([string[]]$Arguments)
    Invoke-Native -Command "mvn" -Arguments $Arguments
}

function Resolve-BashCommand {
    if ($BashCommand) {
        return $BashCommand
    }

    $candidatePaths = @(
        "C:\Program Files\Git\bin\bash.exe",
        "C:\Program Files\Git\usr\bin\bash.exe"
    )
    foreach ($candidate in $candidatePaths) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $commands = @(Get-Command "bash" -All -ErrorAction SilentlyContinue |
        Where-Object { $_.Source -notlike "*\Windows\system32\bash.exe" })
    if ($commands.Count -gt 0) {
        return $commands[0].Source
    }

    throw "Bash syntax check requires Git Bash, WSL bash, or CODE_NEST_BASH pointing to bash.exe"
}

function Test-TextFileHasNul {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    return $bytes -contains 0
}

function Invoke-NulScan {
    $excludedSegments = @(
        "/.git/",
        "/.codegraph/",
        "/.codex-mihomo-geo/",
        "/node_modules/",
        "/target/",
        "/dist/",
        "/build/",
        "/__pycache__/"
    )
    $textExtensions = @(
        ".java", ".xml", ".json", ".md", ".yml", ".yaml", ".js", ".ts", ".vue",
        ".ps1", ".sql", ".properties", ".txt", ".css", ".html", ".csv"
    )
    $violations = New-Object System.Collections.Generic.List[string]

    Get-ChildItem -Path $repoRoot -Recurse -File | ForEach-Object {
        $fullName = $_.FullName
        $normalizedFullName = $fullName.Replace("\", "/")
        $excluded = $false
        foreach ($segment in $excludedSegments) {
            if ($normalizedFullName.Contains($segment)) {
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

    Write-Host "[code-nest-eval] NUL scan passed" -ForegroundColor Green
}

function Test-AllowedSecretPlaceholder {
    param([string]$HitLine)

    $normalized = $HitLine.ToLowerInvariant()
    $placeholderTokens = @(
        "<real-api-key>",
        "<api-key>",
        "secret-key",
        "test-key",
        "fake-key",
        "dummy-key",
        "mock-key",
        "example-key",
        "placeholder",
        "changeme"
    )

    foreach ($token in $placeholderTokens) {
        if ($normalized.Contains($token)) {
            return $true
        }
    }

    return $false
}

function Invoke-Hygiene {
    Invoke-Step "git diff whitespace check" {
        Invoke-Native -Command "git" -Arguments @("diff", "--check")
    }

    Invoke-Step "secret scan" {
        $secretPattern = 'sk-[A-Za-z0-9]{20,}|api[_-]?key[[:space:]]*[:=][[:space:]]*[''"][^''"]+[''"]'
        $secretHits = & git grep -n -I -E -e $secretPattern -- . `
            ":(exclude)target/**" `
            ":(exclude)node_modules/**" `
            ":(exclude).git/**" `
            ":(exclude).codegraph/**" `
            ":(exclude).codex-mihomo-geo/**"
        $grepExitCode = $LASTEXITCODE
        if ($grepExitCode -eq 0) {
            $realSecretHits = @($secretHits | Where-Object { -not (Test-AllowedSecretPlaceholder $_) })
            if ($realSecretHits.Count -gt 0) {
                $realSecretHits | ForEach-Object { Write-Error $_ }
                throw "secret scan found possible secrets"
            }
            Write-Host "[code-nest-eval] secret scan passed with placeholder-only hits" -ForegroundColor Green
        } elseif ($grepExitCode -eq 1) {
            Write-Host "[code-nest-eval] secret scan passed" -ForegroundColor Green
        } else {
            throw "secret scan failed with exit code $grepExitCode"
        }
    }

    Invoke-Step "text NUL-byte scan" {
        Invoke-NulScan
    }
}

function Invoke-Agent {
    $args = @()
    if ($LiveAi -or $LiveWrite) {
        $args += "-LiveAi"
        if ($AiBaseUrl) {
            $args += @("-AiBaseUrl", $AiBaseUrl)
        }
        if ($AiApiKey) {
            $args += @("-AiApiKey", $AiApiKey)
        }
        if ($AiChatModel) {
            $args += @("-AiChatModel", $AiChatModel)
        }
    }
    if ($LiveWrite) {
        $args += "-LiveWrite"
        if ($MysqlUrl) {
            $args += @("-MysqlUrl", $MysqlUrl)
        }
        if ($MysqlUsername) {
            $args += @("-MysqlUsername", $MysqlUsername)
        }
        if ($MysqlPassword) {
            $args += @("-MysqlPassword", $MysqlPassword)
        }
    }
    Invoke-Step "agent runtime eval" {
        & (Join-Path $PSScriptRoot "agent-runtime-eval.ps1") @args
        if ($LASTEXITCODE -ne 0) {
            throw "agent runtime eval failed"
        }
    }
}

function Invoke-Ai {
    Invoke-Step "AI regression suite" {
        Invoke-Maven @(
            "-pl", "xiaou-ai",
            "-am",
            "-Dtest=$($aiRegressionTests -join ',')",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "test"
        )
    }
}

function Invoke-Sre {
    Invoke-Step "deterministic SRE RCA quality gate" {
        Invoke-Maven @(
            "-pl", "xiaou-system",
            "-am",
            "-Dtest=$($sreRegressionTests -join ',')",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "test"
        )
    }
}

function Invoke-Frontend {
    Invoke-Step "frontend node contract tests" {
        Invoke-Native -Command "node" -Arguments (@("--test") + $frontendTests)
    }
}

function Invoke-Rag {
    if ($InstallPythonDeps) {
        Invoke-Step "llamaindex service dependency install" {
            Invoke-Native -Command $PythonCommand -Arguments @("-m", "pip", "install", "-r", "llamaindex-service/requirements.txt")
        }
    }

    Invoke-Step "llamaindex service unittest" {
        Push-Location "llamaindex-service"
        try {
            Invoke-Native -Command $PythonCommand -Arguments @("-m", "unittest", "discover", "-s", "tests", "-v")
        } catch {
            Write-Host "[code-nest-eval] RAG sidecar tests require Python deps. Re-run with -InstallPythonDeps, or set CODE_NEST_PYTHON to a prepared venv interpreter." -ForegroundColor Yellow
            throw
        } finally {
            Pop-Location
        }
    }
}

function Invoke-Backend {
    Invoke-Step "backend module tests" {
        Invoke-Maven @(
            "-pl", ($backendModulesWithTests -join ","),
            "-am",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "test"
        )
    }
}

function Invoke-Release {
    Invoke-Step "backend package without tests" {
        Invoke-Maven @("-B", "-pl", "xiaou-application", "-am", "clean", "package", "-DskipTests")
    }

    Invoke-Step "admin frontend build" {
        Push-Location "vue3-admin-front"
        try {
            Invoke-Native -Command "npm" -Arguments @("run", "build")
        } finally {
            Pop-Location
        }
    }

    Invoke-Step "user frontend build" {
        Push-Location "vue3-user-front"
        try {
            Invoke-Native -Command "npm" -Arguments @("run", "build")
        } finally {
            Pop-Location
        }
    }

    Invoke-Step "docs site build" {
        Push-Location "docs-site"
        try {
            Invoke-Native -Command "npm" -Arguments @("run", "build")
        } finally {
            Pop-Location
        }
    }

    Invoke-Step "script syntax checks" {
        Invoke-Native -Command $PythonCommand -Arguments @(
            "-m", "py_compile",
            "scripts/deploy-frontends.py",
            "scripts/deploy-production.py",
            "scripts/sre-alertmanager-e2e.py",
            "scripts/test_sre_alertmanager_e2e.py",
            "scripts/you_deserve_to_interview_sql.py"
        )
        $bash = Resolve-BashCommand
        Invoke-Native -Command $bash -Arguments @(
            "-n",
            "scripts/ci-server-build-deploy.sh",
            "scripts/ci-server-build-deploy.test.sh",
            "scripts/deploy-release.sh",
            "scripts/deploy-release.test.sh",
            "scripts/external-uptime-check.sh",
            "scripts/external-uptime-check.test.sh",
            "scripts/server-capacity-governance.sh",
            "scripts/server-capacity-governance.test.sh",
            "scripts/verify-production-baseline.sh",
            "scripts/verify-production-baseline.test.sh"
        )
    }

    Invoke-Step "release script contract tests" {
        $bash = Resolve-BashCommand
        foreach ($contractTest in @(
            "scripts/ci-server-build-deploy.test.sh",
            "scripts/deploy-release.test.sh",
            "scripts/external-uptime-check.test.sh",
            "scripts/server-capacity-governance.test.sh",
            "scripts/verify-production-baseline.test.sh"
        )) {
            Invoke-Native -Command $bash -Arguments @($contractTest)
        }
        Invoke-Native -Command $PythonCommand -Arguments @("scripts/test_sre_alertmanager_e2e.py")
    }
}

switch ($Tier) {
    "hygiene" {
        Invoke-Hygiene
    }
    "smoke" {
        Invoke-Step "frontend node contract tests" {
            Invoke-Native -Command "node" -Arguments (@("--test") + $frontendTests)
        }
        Invoke-Agent
    }
    "agent" {
        Invoke-Agent
    }
    "ai" {
        Invoke-Ai
    }
    "sre" {
        Invoke-Sre
    }
    "frontend" {
        Invoke-Frontend
    }
    "rag" {
        Invoke-Rag
    }
    "backend" {
        Invoke-Backend
    }
    "release" {
        Invoke-Release
    }
    "all" {
        Invoke-Hygiene
        Invoke-Frontend
        Invoke-Rag
        Invoke-Ai
        Invoke-Sre
        Invoke-Backend
        Invoke-Agent
    }
}

Write-Host ""
Write-Host "[code-nest-eval] tier '$Tier' passed" -ForegroundColor Green
