param(
  [switch]$RequireCodex
)

$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\Initialize-StreamoraToolchain.ps1" -RequireCodex:$RequireCodex | Out-Null

$expectedRoots = @{
  node = 'D:\aitool\node'
  pnpm = 'D:\aitool\node'
  mvn = 'D:\aitool\maven\bin'
  gh = 'D:\aitool\gh\bin'
}
if ($RequireCodex) {
  $expectedRoots.codex = 'D:\aitool\codex'
}

foreach ($commandName in $expectedRoots.Keys) {
  $command = Get-Command $commandName -ErrorAction Stop
  $actualSource = [System.IO.Path]::GetFullPath($command.Source)
  $expectedRoot = [System.IO.Path]::GetFullPath($expectedRoots[$commandName]).TrimEnd('\') + '\'
  if (-not $actualSource.StartsWith($expectedRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "$commandName resolved to $actualSource instead of a launcher under $expectedRoot."
  }
}

$versions = [ordered]@{
  node = (& node --version).Trim()
  pnpm = (& pnpm --version).Trim()
  maven = ((& mvn --version | Select-Object -First 1) -replace '^Apache Maven ', '').Trim()
  mavenWrapper = ((& "$PSScriptRoot\..\..\mvnw.cmd" --version | Select-Object -First 1) -replace '^Apache Maven ', '').Trim()
  gh = (& gh --version | Select-Object -First 1).Trim()
}
$expectedMavenRepository = 'D:\aitool\maven-cache\repository'
if ($env:MAVEN_OPTS -notmatch [regex]::Escape($expectedMavenRepository)) {
  throw "Maven repository cache is not pinned to $expectedMavenRepository."
}
if (-not (Test-Path -LiteralPath (Join-Path $env:MAVEN_USER_HOME 'wrapper'))) {
  throw "Maven Wrapper cache is missing from $env:MAVEN_USER_HOME."
}
$expectedPnpmStore = 'D:\aitool\pnpm\store\v11'
$actualPnpmStore = (& pnpm store path).Trim()
if ($actualPnpmStore -ne $expectedPnpmStore) {
  throw "pnpm store resolved to $actualPnpmStore instead of $expectedPnpmStore."
}
if ($RequireCodex) {
  $versions.codex = (& codex --version).Trim()
}

[PSCustomObject]@{
  Versions = $versions
  CorepackHome = $env:COREPACK_HOME
  PnpmStore = $actualPnpmStore
  PnpmCache = $env:npm_config_cache
  MavenUserHome = $env:MAVEN_USER_HOME
  MavenOptions = $env:MAVEN_OPTS
  GitHubCliConfig = $env:GH_CONFIG_DIR
  CodexHome = $env:CODEX_HOME
}
