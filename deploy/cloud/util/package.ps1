# LeoMall 云端部署包组装脚本（在开发机 Windows / pwsh 7 上运行）
#
# 用法（仓库任意位置）：
#   pwsh -File deploy\cloud\util\package.ps1                  # 全量：前端 + 后端 + 组装
#   pwsh -File deploy\cloud\util\package.ps1 -ExportCatalog   # 额外从本机 MySQL 重新导出商品快照
#   pwsh -File deploy\cloud\util\package.ps1 -SkipFrontend    # 跳过前端构建（复用已有 dist）
#   pwsh -File deploy\cloud\util\package.ps1 -SkipBackend     # 跳过后端构建（复用已有 jar）
#
# 产物：deploy\cloud\package\  —— 整个目录上传到服务器 /opt/leomall 即可 compose up
# 依赖：Node 18+、JDK 8 + Maven、（可选 -ExportCatalog 时）uv + mysql 客户端 + 本机 MySQL
param(
    [switch]$SkipFrontend,
    [switch]$SkipBackend,
    [switch]$ExportCatalog
)
$ErrorActionPreference = 'Stop'

$CloudDir = Split-Path -Parent $PSScriptRoot                 # deploy\cloud
$RepoRoot = (Resolve-Path (Join-Path $CloudDir '..\..')).Path # 仓库根
$Pkg      = Join-Path $CloudDir 'package'
Write-Host "仓库根：$RepoRoot`n输出包：$Pkg"

# ---------- 1. 前端构建（cloud 模式：同源相对 API 路径） ----------
if (-not $SkipFrontend) {
    $env:NODE_OPTIONS = '--openssl-legacy-provider'   # Node 18+ 跑 webpack4 必需
    foreach ($app in @('admin', 'h5')) {
        $dir = Join-Path $RepoRoot $app
        Write-Host "`n===== 构建 $app ====="
        Push-Location $dir
        try {
            if (-not (Test-Path -LiteralPath (Join-Path $dir 'node_modules'))) {
                & npm install
                if ($LASTEXITCODE -ne 0) { throw "$app npm install 失败" }
            }
            & npx vue-cli-service build --mode cloud
            if ($LASTEXITCODE -ne 0) { throw "$app 前端构建失败" }
        } finally { Pop-Location }
    }
}

# ---------- 2. 后端构建 ----------
if (-not $SkipBackend) {
    Write-Host "`n===== 构建后端（需 JDK 8） ====="
    Push-Location (Join-Path $RepoRoot 'server')
    try {
        if (Test-Path Env:SERVER__PORT) { Remove-Item Env:SERVER__PORT }
        & mvn clean package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Maven 构建失败' }
    } finally { Pop-Location }
}

# ---------- 3. 商品快照 ----------
$AgentDir = Join-Path $RepoRoot 'agent'
if ($ExportCatalog) {
    Write-Host "`n===== 从本机 MySQL 导出商品快照 ====="
    Push-Location $AgentDir
    try {
        & uv run python scripts/export_leomall_catalog.py
        if ($LASTEXITCODE -ne 0) { throw '商品快照导出失败（确认 MySQL 已启动、LEOMALL_MYSQL_* 环境变量正确）' }
    } finally { Pop-Location }
}
$Catalog = Join-Path $AgentDir 'data\leomall_catalog.json'
if (-not (Test-Path -LiteralPath $Catalog)) {
    throw "缺少商品快照 $Catalog；先运行 agent 导出脚本或加 -ExportCatalog"
}

# ---------- 4. 组装上传包 ----------
Write-Host "`n===== 组装 package/ ====="
foreach ($sub in @('leomall', 'www', 'agent', 'agent-data', 'db\init-sql', 'db\conf.d', 'nginx')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $Pkg $sub) | Out-Null
}

# 编排与配置
Copy-Item -LiteralPath (Join-Path $CloudDir 'docker-compose.yml') -Destination $Pkg
Copy-Item -LiteralPath (Join-Path $CloudDir '.env.example')     -Destination $Pkg
Copy-Item -LiteralPath (Join-Path $CloudDir 'nginx\leomall.conf') -Destination (Join-Path $Pkg 'nginx')
Copy-Item -LiteralPath (Join-Path $CloudDir 'leomall\Dockerfile') -Destination (Join-Path $Pkg 'leomall')
Copy-Item -LiteralPath (Join-Path $CloudDir 'db\conf.d\my.cnf')   -Destination (Join-Path $Pkg 'db\conf.d')

# 后端 jar
$Jar = Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'server\leomall-all\target') -Filter 'leomall-all-*-exec.jar' |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $Jar) { throw '找不到 leomall-all-*-exec.jar，先完成后端构建' }
Copy-Item -LiteralPath $Jar.FullName -Destination (Join-Path $Pkg 'leomall\leomall.jar')

# 前端产物
foreach ($app in @('admin', 'h5')) {
    $dist = Join-Path $RepoRoot "$app\dist"
    if (-not (Test-Path -LiteralPath $dist)) { throw "缺少 $dist，先完成前端构建" }
    $dest = Join-Path $Pkg "www\$app"
    if (Test-Path -LiteralPath $dest) { Remove-Item -LiteralPath $dest -Recurse -Force }
    Copy-Item -LiteralPath $dist -Destination $dest -Recurse
}

# Agent 源码（Dockerfile 构建所需最小子集，不带 .venv / data / tests）
foreach ($item in @('app', 'knowledge')) {
    $dest = Join-Path $Pkg "agent\$item"
    if (Test-Path -LiteralPath $dest) { Remove-Item -LiteralPath $dest -Recurse -Force }
    Copy-Item -LiteralPath (Join-Path $AgentDir $item) -Destination $dest -Recurse
}
foreach ($file in @('pyproject.toml', 'uv.lock', 'README.md', 'Dockerfile', '.python-version')) {
    Copy-Item -LiteralPath (Join-Path $AgentDir $file) -Destination (Join-Path $Pkg 'agent')
}
Get-ChildItem -LiteralPath (Join-Path $Pkg 'agent') -Recurse -Directory -Filter '__pycache__' |
    Remove-Item -Recurse -Force

# 商品快照
Copy-Item -LiteralPath $Catalog -Destination (Join-Path $Pkg 'agent-data\leomall_catalog.json')

# 数据库初始化脚本（合并为一个文件；UTF-8 无 BOM，MySQL 才能正确解析首行）
$SqlParts = foreach ($name in @('leomall_schema.sql', 'leomall_table.sql', 'leomall_data.sql')) {
    [IO.File]::ReadAllText((Join-Path $RepoRoot "sql\$name"), [Text.Encoding]::UTF8)
}
[IO.File]::WriteAllText(
    (Join-Path $Pkg 'db\init-sql\leomall.sql'),
    ($SqlParts -join "`n"),
    [Text.UTF8Encoding]::new($false)
)

# 包内说明与忽略规则
[IO.File]::WriteAllText((Join-Path $Pkg '.gitignore'), "*`n!.gitignore`n", [Text.UTF8Encoding]::new($false))

$sizeMB = [math]::Round((Get-ChildItem -LiteralPath $Pkg -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)
Write-Host @"

===== 完成：$Pkg（$sizeMB MB） =====
下一步：
  1. 上传整包到服务器：
     scp -r "$Pkg" ubuntu@<服务器IP>:/opt/leomall
  2. 服务器上：cd /opt/leomall && cp .env.example .env && vim .env
  3. docker compose up -d --build
  详细步骤见 deploy/cloud/README.md
"@