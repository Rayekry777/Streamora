[CmdletBinding()]
param(
  [string]$KeyPath = (Join-Path $env:USERPROFILE '.ssh\streamora_vm'),
  [string]$UserName = 'ub001'
)

$ErrorActionPreference = 'Stop'
$sshOptions = @('-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=10', '-i', $KeyPath)
$targets = @(
  @{ Name = 'DEV'; Host = '192.168.126.129'; Project = 'streamora-dev' },
  @{ Name = 'SIM'; Host = '192.168.126.128'; Project = 'streamora' }
)

foreach ($target in $targets) {
  Write-Output "[$($target.Name)] $($target.Host) project=$($target.Project)"
  & ssh @sshOptions "$UserName@$($target.Host)" "docker ps -a --filter label=com.docker.compose.project=$($target.Project) --format '{{.Names}}`t{{.Image}}`t{{.Status}}'"
  if ($LASTEXITCODE -ne 0) { throw "$($target.Name) Docker 状态读取失败。" }
  Write-Output ''
}
