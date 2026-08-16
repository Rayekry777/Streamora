$ErrorActionPreference = 'Stop'

$source = 'E:\Nodejsnew'
$destination = 'D:\aitool\node'
$toolRoot = 'D:\aitool'
$corepackHome = Join-Path $toolRoot 'pnpm'

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = [Security.Principal.WindowsPrincipal]::new($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
  throw 'Run this script from an elevated Administrator PowerShell window.'
}

if (-not (Test-Path -LiteralPath (Join-Path $source 'node.exe'))) {
  throw "Node source is missing: $source"
}

if (Test-Path -LiteralPath $destination) {
  $existingItems = @(Get-ChildItem -LiteralPath $destination -Force)
  if ($existingItems.Count -gt 0) {
    throw "Refusing to replace a non-empty destination: $destination"
  }

  # This empty directory is the known residue of the previously failed move.
  Remove-Item -LiteralPath $destination -Force
}

Move-Item -LiteralPath $source -Destination $destination
if ((Test-Path -LiteralPath $source) -or -not (Test-Path -LiteralPath (Join-Path $destination 'node.exe'))) {
  throw 'Node was not relocated exclusively to D:\aitool\node.'
}

$env:COREPACK_HOME = $corepackHome
New-Item -ItemType Directory -Force -Path $corepackHome | Out-Null
& (Join-Path $destination 'corepack.cmd') enable --install-directory $destination
& (Join-Path $destination 'corepack.cmd') install --global 'pnpm@11.19.0'

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries = @($userPath -split ';' | Where-Object { $_ -and $_.TrimEnd('\\') -ne 'E:\Nodejsnew' })
if ($entries -notcontains $destination) {
  $entries = @($destination) + $entries
}
[Environment]::SetEnvironmentVariable('Path', ($entries -join ';'), 'User')

[PSCustomObject]@{
  Node = (& (Join-Path $destination 'node.exe') --version).Trim()
  Pnpm = (& (Join-Path $destination 'pnpm.cmd') --version).Trim()
  NodeHome = $destination
  CorepackHome = $corepackHome
}
