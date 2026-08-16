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
$healthContainers = @(
  'streamora-dev-postgres-1',
  'streamora-dev-redis-1',
  'streamora-dev-nacos-1',
  'streamora-dev-rocketmq-namesrv-1',
  'streamora-dev-rocketmq-broker-1',
  'streamora-dev-minio-1'
) -join ' '
$remoteCommand = "set -eu; $compose --profile infra config --quiet; $compose --profile infra ps --status running --services; for container in $healthContainers; do docker inspect -f '{{.State.Health.Status}}' `$container | grep -Fx healthy; done; $compose --profile infra exec -T redis redis-cli ping | grep -Fx PONG; $compose --profile infra exec -T postgres pg_isready -U streamora_platform_admin -d streamora"
& ssh @sshOptions $sshTarget $remoteCommand
if ($LASTEXITCODE -ne 0) { throw '开发 VM 容器或内部连接检查失败。' }

$checks = @(
  @{ Name = 'PostgreSQL'; Port = 5432 },
  @{ Name = 'Redis'; Port = 6379 },
  @{ Name = 'Nacos HTTP'; Port = 8848 },
  @{ Name = 'Nacos gRPC'; Port = 9848 },
  @{ Name = 'Nacos gRPC client'; Port = 9849 },
  @{ Name = 'RocketMQ NameServer'; Port = 9876 },
  @{ Name = 'RocketMQ Broker'; Port = 10911 },
  @{ Name = 'MinIO API'; Port = 9000 },
  @{ Name = 'MinIO Console'; Port = 9001 }
)
foreach ($check in $checks) {
  $result = Test-NetConnection -ComputerName $HostName -Port $check.Port -WarningAction SilentlyContinue
  if (-not $result.TcpTestSucceeded) { throw "$($check.Name) 未监听 $HostName`:$($check.Port)" }
  Write-Output "$($check.Name) $HostName`:$($check.Port) reachable"
}

$readiness = Invoke-WebRequest -UseBasicParsing -Uri "http://$HostName`:8848/nacos/v3/admin/core/state/readiness" -TimeoutSec 10
if ($readiness.StatusCode -lt 200 -or $readiness.StatusCode -ge 300) { throw 'Nacos readiness 返回非成功状态。' }
Write-Output 'Nacos readiness passed'
