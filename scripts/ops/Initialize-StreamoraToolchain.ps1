param(
  [switch]$RequireCodex
)

$ErrorActionPreference = 'Stop'

$toolRoot = 'D:\aitool'
$nodeHome = Join-Path $toolRoot 'node'
$mavenHome = Join-Path $toolRoot 'maven'
$ghHome = Join-Path $toolRoot 'gh'
$codexHome = Join-Path $toolRoot 'codex'
$pnpmHome = Join-Path $toolRoot 'pnpm'

$requiredTools = @(
  @{ Name = 'Node'; Path = Join-Path $nodeHome 'node.exe' },
  @{ Name = 'npm'; Path = Join-Path $nodeHome 'npm.cmd' },
  @{ Name = 'Corepack'; Path = Join-Path $nodeHome 'corepack.cmd' },
  @{ Name = 'pnpm'; Path = Join-Path $nodeHome 'pnpm.cmd' },
  @{ Name = 'Maven'; Path = Join-Path $mavenHome 'bin\mvn.cmd' },
  @{ Name = 'GitHub CLI'; Path = Join-Path $ghHome 'bin\gh.exe' }
)

if ($RequireCodex) {
  $requiredTools += @{ Name = 'Codex CLI'; Path = Join-Path $codexHome 'codex.cmd' }
}

foreach ($tool in $requiredTools) {
  if (-not (Test-Path -LiteralPath $tool.Path)) {
    throw "$($tool.Name) is missing from the Streamora toolchain: $($tool.Path)"
  }
}

$env:COREPACK_HOME = $pnpmHome
$env:PNPM_HOME = $nodeHome
$env:npm_config_store_dir = Join-Path $pnpmHome 'store'
$env:npm_config_cache = Join-Path $pnpmHome 'cache'
$env:MAVEN_HOME = $mavenHome
$env:MAVEN_USER_HOME = Join-Path $toolRoot 'maven-cache'
$mavenRepository = Join-Path $env:MAVEN_USER_HOME 'repository'
$env:GH_CONFIG_DIR = Join-Path $ghHome 'config'
$env:CODEX_HOME = Join-Path $codexHome 'home'

$mavenRepositoryOption = "-Dmaven.repo.local=$mavenRepository"
if ($env:MAVEN_OPTS -notmatch [regex]::Escape($mavenRepositoryOption)) {
  $env:MAVEN_OPTS = "$mavenRepositoryOption $($env:MAVEN_OPTS)".Trim()
}

foreach ($stateDirectory in @(
  $env:COREPACK_HOME,
  $env:npm_config_store_dir,
  $env:npm_config_cache,
  $env:MAVEN_USER_HOME,
  $mavenRepository,
  $env:GH_CONFIG_DIR,
  $env:CODEX_HOME
)) {
  New-Item -ItemType Directory -Force -Path $stateDirectory | Out-Null
}

$toolPaths = @(
  $nodeHome,
  (Join-Path $mavenHome 'bin'),
  (Join-Path $ghHome 'bin'),
  $codexHome
)
$env:PATH = ($toolPaths + ($env:PATH -split ';' | Where-Object { $_ })) -join ';'

[PSCustomObject]@{
  ToolRoot = $toolRoot
  NodeHome = $nodeHome
  MavenHome = $mavenHome
  MavenUserHome = $env:MAVEN_USER_HOME
  CorepackHome = $env:COREPACK_HOME
  GitHubCliHome = $ghHome
  CodexHome = $codexHome
}
