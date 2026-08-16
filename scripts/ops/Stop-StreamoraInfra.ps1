[CmdletBinding()]
param(
  [string]$HostName = '192.168.126.129',
  [string]$UserName = 'ub001',
  [string]$KeyPath = (Join-Path $env:USERPROFILE '.ssh\streamora_vm'),
  [string]$RemoteRoot = '/home/ub001/streamora'
)

$ErrorActionPreference = 'Stop'
$sshTarget = "$UserName@$HostName"
$sshOptions = @('-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=10', '-i', $KeyPath)
if (-not (Test-Path -LiteralPath $KeyPath)) { throw "SSH 私钥不存在：$KeyPath" }

$compose = "docker compose --project-name streamora-dev --project-directory $RemoteRoot/platform/compose --env-file $RemoteRoot/platform/compose/.env.dev -f $RemoteRoot/platform/compose/compose.yml -f $RemoteRoot/platform/compose/compose.dev-vm.yml"
$services = @('postgres', 'postgres-privileges-init', 'redis', 'nacos', 'rocketmq-namesrv', 'rocketmq-volume-init', 'rocketmq-broker', 'minio') -join ' '
& ssh @sshOptions $sshTarget "set -eu; $compose --profile infra stop $services"
if ($LASTEXITCODE -ne 0) { throw '开发 VM 中间件停止失败。' }
