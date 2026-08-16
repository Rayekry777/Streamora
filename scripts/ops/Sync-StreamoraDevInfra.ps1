[CmdletBinding()]
param(
  [string]$HostName = '192.168.126.129',
  [string]$UserName = 'ub001',
  [string]$KeyPath = (Join-Path $env:USERPROFILE '.ssh\streamora_vm'),
  [string]$RemoteRoot = '/home/ub001/streamora'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$sourcePlatform = Join-Path $repositoryRoot 'platform'
$sshTarget = "$UserName@$HostName"
$sshOptions = @('-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=10', '-i', $KeyPath)
$scpOptions = @('-r', '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=10', '-i', $KeyPath)

if (-not (Test-Path -LiteralPath $KeyPath)) { throw "SSH 私钥不存在：$KeyPath" }
if (-not (Test-Path -LiteralPath $sourcePlatform)) { throw "平台目录不存在：$sourcePlatform" }

& ssh @sshOptions $sshTarget "mkdir -p $RemoteRoot"
if ($LASTEXITCODE -ne 0) { throw '无法准备开发 VM 工作目录。' }
& scp @scpOptions $sourcePlatform "$sshTarget`:$RemoteRoot"
if ($LASTEXITCODE -ne 0) { throw '平台 Compose 文件上传失败。' }

Write-Output "开发 VM 平台文件已同步到 $RemoteRoot/platform"
