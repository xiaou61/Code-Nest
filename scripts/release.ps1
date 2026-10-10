<#
  一条命令发版。目标：一分钟以内（每步耗时都会打出来）。

  流程：本地增量打包 -> 只打一个 tar -> 传到服务器 -> 解包 -> 重启 -> 健康检查。

  三条设计约束：
  1. **脚本里不写任何凭据与服务器地址**：连服务器走 quick-server 的别名，凭据从 Windows 凭据管理器取。
  2. **发版不碰服务器上的 application.yml**：那份配置（数据源口令、JWT 密钥、SMTP 授权码、端口）
     只存在于服务器上，用它自己的值。用本地副本覆盖它会把端口与库名改回开发值。
  3. **不依赖增量编译的正确性**：打包前先删掉 `target/classes/db`，强制资源目录重新拷贝。
     不这么做的话，删掉的迁移文件会留在 target 里继续被打进包（2026-10-10 真踩过：
     两个临时探针迁移被带上了部署）。

  用法：
    powershell -File scripts/release.ps1                 # 后端 + 前端
    powershell -File scripts/release.ps1 -SkipWeb        # 只改后端时（更快）
    powershell -File scripts/release.ps1 -WithTests      # 发版前先跑全量后端测试
    powershell -File scripts/release.ps1 -Server <别名>  # 默认 codenest-online
#>
param(
    [switch]$SkipWeb,
    [switch]$WithTests,
    [string]$Server = 'codenest-online',
    [int]$Port = 8081,
    [string]$RemoteDir = '/opt/paideia-app',
    [string]$QuickServerScript = (Join-Path $HOME '.codex\skills\quick-server\scripts\connect.ps1')
)

$ErrorActionPreference = 'Stop'
$total = [System.Diagnostics.Stopwatch]::StartNew()
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$staging = Join-Path $repo 'backend\paideia-app\target\release-staging'
$archive = Join-Path $repo 'backend\paideia-app\target\release.tar.gz'

if (-not (Test-Path $QuickServerScript)) { throw "找不到 quick-server 脚本：$QuickServerScript" }

function Step([string]$name, [scriptblock]$action) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & $action
    $sw.Stop()
    Write-Host ("  {0,-28} {1,6:N1}s" -f $name, ($sw.Elapsed.TotalMilliseconds / 1000))
}

function Remote([string]$command) {
    & powershell -ExecutionPolicy Bypass -File $QuickServerScript -Server $Server -Command $command | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "远端命令失败（退出码 $LASTEXITCODE）：$command" }
}

function Upload([string]$local, [string]$remote) {
    $env:MSYS_NO_PATHCONV = '1'
    try {
        & powershell -ExecutionPolicy Bypass -File $QuickServerScript -Server $Server -Upload $local -RemotePath $remote | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "上传失败（退出码 $LASTEXITCODE）：$local" }
    } finally { Remove-Item Env:\MSYS_NO_PATHCONV -ErrorAction SilentlyContinue }
}

Write-Host "发版到 $Server（端口 $Port，目录 $RemoteDir）"

if ($WithTests) {
    Step '后端全量测试' {
        Push-Location (Join-Path $repo 'backend')
        try { & mvn -B verify | Out-Null } finally { Pop-Location }
    }
}

Step '后端打包（增量）' {
    Push-Location (Join-Path $repo 'backend')
    try {
        Remove-Item (Join-Path $repo 'backend\paideia-app\target\classes\db') -Recurse -Force -ErrorAction SilentlyContinue
        & mvn -B package -DskipTests | Out-Null
    } finally { Pop-Location }
}

if (-not $SkipWeb) {
    Step '前端构建（学习者端 + 管理端）' {
        Push-Location (Join-Path $repo 'frontend')
        try {
            & pnpm --filter '@paideia/app' build | Out-Null
            & pnpm --filter '@paideia/admin' build | Out-Null
        } finally { Pop-Location }
    }
} else {
    Write-Host '  （跳过前端构建：-SkipWeb）'
}

Step '打包 staging 并压缩' {
    Remove-Item $staging -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path (Join-Path $staging 'web\admin') -Force | Out-Null
    Copy-Item (Join-Path $repo 'backend\paideia-app\target\paideia-app-0.0.1-SNAPSHOT.jar') (Join-Path $staging 'app.jar')
    Copy-Item (Join-Path $repo 'frontend\apps\app\dist\*') (Join-Path $staging 'web') -Recurse -Force
    Copy-Item (Join-Path $repo 'frontend\apps\admin\dist\*') (Join-Path $staging 'web\admin') -Recurse -Force
    Remove-Item $archive -Force -ErrorAction SilentlyContinue
    # 必须用 Windows 自带的 bsdtar：从 Git Bash 继承 PATH 时会拿到 GNU tar，
    # 它把 `F:\...` 当成"远程主机:路径"并报 Cannot connect to F: resolve failed。
    $bsdtar = Join-Path $env:SystemRoot 'System32\tar.exe'
    if (-not (Test-Path $bsdtar)) { throw "找不到 $bsdtar" }
    & $bsdtar -czf $archive -C $staging .
    if ($LASTEXITCODE -ne 0) { throw '压缩失败' }
    if (-not (Test-Path $archive)) { throw '压缩产物不存在' }
}

Step '上传（一次 scp）' { Upload $archive "$RemoteDir/release.tar.gz" }

Step '远端解包' {
    Remote "cd $RemoteDir && tar xzf release.tar.gz && rm -f release.tar.gz && echo extracted"
}

Step '重启服务' { Remote 'systemctl restart paideia' }

Step '等待健康检查' {
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 2
        $out = & powershell -ExecutionPolicy Bypass -File $QuickServerScript -Server $Server `
            -Command "curl -fsS -m 4 http://127.0.0.1:$Port/actuator/health" 2>$null
        if ($out -match '"status":"UP"') { return }
    }
    throw '健康检查超时：服务未在 60 秒内就绪'
}

$total.Stop()
Write-Host ("完成：{0:N1} 秒" -f $total.Elapsed.TotalSeconds)
