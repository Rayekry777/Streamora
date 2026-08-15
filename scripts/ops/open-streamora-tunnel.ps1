param(
  [string]$HostName = '192.168.126.128',
  [string]$UserName = 'ub001',
  [string]$KeyPath = "$env:USERPROFILE\.ssh\streamora_vm",
  [int]$WebPort = 3000,
  [int]$AdminWebPort = 3001
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $KeyPath)) {
  throw "SSH private key does not exist: $KeyPath"
}

ssh -N -o ExitOnForwardFailure=yes -i $KeyPath `
  -L "$WebPort`:127.0.0.1:3000" `
  -L "$AdminWebPort`:127.0.0.1:3001" `
  "$UserName@$HostName"
