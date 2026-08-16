[CmdletBinding()]
param(
  [string]$HostName = '192.168.126.129',
  [string]$UserName = 'ub001',
  [string]$KeyPath = (Join-Path $env:USERPROFILE '.ssh\streamora_vm'),
  [string]$RemoteRoot = '/home/ub001/streamora'
)

$ErrorActionPreference = 'Stop'
$syncScript = Join-Path $PSScriptRoot 'Sync-StreamoraDevInfra.ps1'
$sshTarget = "$UserName@$HostName"
$sshOptions = @('-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=10', '-i', $KeyPath)

if (-not (Test-Path -LiteralPath $KeyPath)) { throw "SSH 私钥不存在：$KeyPath" }
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { throw '找不到 ssh.exe。' }
if (-not (Get-Command scp -ErrorAction SilentlyContinue)) { throw '找不到 scp.exe。' }

& $syncScript -HostName $HostName -UserName $UserName -KeyPath $KeyPath -RemoteRoot $RemoteRoot
if ($LASTEXITCODE -ne 0) { throw '开发 VM Compose 文件同步失败。' }

$compose = "docker compose --project-name streamora-dev --project-directory $RemoteRoot/platform/compose --env-file $RemoteRoot/platform/compose/.env.dev -f $RemoteRoot/platform/compose/compose.yml -f $RemoteRoot/platform/compose/compose.dev-vm.yml"
$services = @('postgres', 'postgres-privileges-init', 'redis', 'nacos', 'rocketmq-namesrv', 'rocketmq-volume-init', 'rocketmq-broker', 'minio') -join ' '
$remoteCommand = "set -eu; $compose --profile infra config --quiet; $compose --profile infra up -d $services; $compose --profile infra ps $services"

& ssh @sshOptions $sshTarget $remoteCommand
if ($LASTEXITCODE -ne 0) { throw '开发 VM 中间件启动失败。' }

$testScript = Join-Path $PSScriptRoot 'Test-StreamoraInfra.ps1'
for ($attempt = 1; $attempt -le 60; $attempt++) {
  try {
    & $testScript -HostName $HostName -UserName $UserName -KeyPath $KeyPath -RemoteRoot $RemoteRoot
    if ($LASTEXITCODE -eq 0) { return }
  } catch {
    if ($attempt -eq 60) { throw }
  }
  Start-Sleep -Seconds 5
}
throw '开发 VM 中间件在 5 分钟内未达到健康状态。'
