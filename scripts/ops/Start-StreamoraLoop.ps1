[CmdletBinding(DefaultParameterSetName = 'Watch')]
param(
  [Parameter(Mandatory, ParameterSetName = 'Pr')]
  [int]$Pr,
  [Parameter(Mandatory, ParameterSetName = 'Issue')]
  [int]$Issue,
  [Parameter(ParameterSetName = 'Issue')]
  [ValidateRange(1, 8)]
  [int]$StagePhase,
  [Parameter(ParameterSetName = 'Issue')]
  [switch]$AllowFirstPush,
  [Parameter(ParameterSetName = 'Watch')]
  [switch]$Watch,
  [ValidateRange(30, 900)]
  [int]$PollSeconds = 120,
  [switch]$Once
)

$ErrorActionPreference = 'Stop'
if ($PSCmdlet.ParameterSetName -eq 'Issue' -and -not $AllowFirstPush.IsPresent) {
  throw "Issue 自动任务的首次推送尚未授权；确认后使用 -AllowFirstPush 重新启动。"
}
$script:LoopWatchMode = $PSCmdlet.ParameterSetName -eq 'Watch'

. "$PSScriptRoot\Initialize-StreamoraToolchain.ps1" -RequireCodex | Out-Null

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$loopRoot = 'D:\aitool\loop-state'
$controlledClone = Join-Path $loopRoot 'repository'
$taskDirectory = Join-Path $loopRoot 'tasks'
$reportDirectory = Join-Path $loopRoot 'reports'
$mutex = [Threading.Mutex]::new($false, 'Local\StreamoraLoopEngineering')

foreach ($directory in @($loopRoot, $taskDirectory, $reportDirectory)) {
  New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

function Invoke-Native {
  param(
    [Parameter(Mandatory)] [string]$File,
    [Parameter()] [string[]]$Arguments = @(),
    [Parameter()] [string]$WorkingDirectory = $null
  )

  Push-Location
  try {
    if ($WorkingDirectory) {
      Set-Location $WorkingDirectory
    }
    $output = (& $File @Arguments 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
      throw "$File $($Arguments -join ' ') failed: $output"
    }
    return $output
  } finally {
    Pop-Location
  }
}

function Invoke-GhJson {
  param([string[]]$Arguments)
  return (Invoke-Native -File 'gh' -Arguments $Arguments | ConvertFrom-Json)
}

function Get-RepositoryName {
  return (Invoke-GhJson @('repo', 'view', '--json', 'nameWithOwner')).nameWithOwner
}

function Ensure-LoopLabels {
  $labels = @(
    @{ name = 'agent:ready'; color = '0E8A16'; description = '允许本机 Loop 创建实现 PR' },
    @{ name = 'agent:running'; color = '1D76DB'; description = '本机 Loop 正在跟踪' },
    @{ name = 'agent:repairing'; color = 'FBCA04'; description = '本机 Codex 正在修复' },
    @{ name = 'agent:root-cause'; color = 'B60205'; description = '本机 Loop 正在根因分析' },
    @{ name = 'agent:blocked'; color = 'D93F0B'; description = '本机 Loop 已停止自动写入' },
    @{ name = 'agent:done'; color = '0E8A16'; description = '本机 Loop 已完成' },
    @{ name = 'stage:ready'; color = '5319E7'; description = '授权当前阶段 PR Head SHA 执行虚拟机验收' }
  )
  $existing = @(Invoke-GhJson @('label', 'list', '--limit', '100', '--json', 'name') | ForEach-Object { $_.name })
  foreach ($label in $labels) {
    if ($existing -notcontains $label.name) {
      Invoke-Native -File 'gh' -Arguments @('label', 'create', $label.name, '--color', $label.color, '--description', $label.description) | Out-Null
    }
  }
}

function Get-StagePhase {
  param([string]$Branch)
  if ($Branch -match '^feature/phase-(\d+)-.+' -or $Branch -match '^agent/.+-phase-(\d+)-.+') {
    return [int]$Matches[1]
  }
  return $null
}

function Test-AuthorizedLoopRepairCommit {
  param([pscustomobject]$State, [string]$HeadSha)
  if ([int]$State.repairAttempt -lt 1 -or [int]$State.repairAttempt -gt 3) {
    return $false
  }
  $message = Invoke-Native -File 'gh' -Arguments @('api', "repos/$(Get-RepositoryName)/commits/$HeadSha", '--jq', '.commit.message')
  $root = [regex]::Escape($State.rootSha)
  $attempt = [regex]::Escape([string]$State.repairAttempt)
  return ($message -match "(?m)^Streamora-Loop-Root:\s*$root\s*$" -and
    $message -match "(?m)^Streamora-Loop-Attempt:\s*$attempt\s*$" -and
    $message -match '(?m)^Streamora-Loop-Mode:\s*ci-repair\s*$')
}

function Update-LoopStateSchema {
  param([pscustomobject]$State)
  $defaults = [ordered]@{
    phaseNumber = (Get-StagePhase $State.branch)
    stageAuthorizedSha = $null
    stageRunId = $null
    upgradeResult = $null
    cleanInstallResult = $null
    imageDigest = $null
    acceptanceRunUrl = $null
  }
  foreach ($entry in $defaults.GetEnumerator()) {
    if ($State.PSObject.Properties.Name -notcontains $entry.Key) {
      $State | Add-Member -NotePropertyName $entry.Key -NotePropertyValue $entry.Value
    }
  }
  return $State
}

function Get-TaskPath {
  param([string]$Kind, [string]$Reference)
  return (Join-Path $taskDirectory "$Kind-$Reference.json")
}

function Save-LoopState {
  param([pscustomobject]$State)
  $State.updatedAt = (Get-Date).ToUniversalTime().ToString('o')
  $path = Get-TaskPath $State.kind $State.reference
  $State | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $path -Encoding utf8
  return $path
}

function Get-LoopState {
  param([string]$Kind, [string]$Reference)
  $path = Get-TaskPath $Kind $Reference
  if (-not (Test-Path -LiteralPath $path)) {
    return $null
  }
  return (Update-LoopStateSchema (Get-Content -Raw -LiteralPath $path | ConvertFrom-Json))
}

function New-LoopState {
  param([string]$Kind, [string]$Reference, [string]$RootSha, [string]$Branch, [Nullable[int]]$SourceIssue = $null)
  $stagePhase = Get-StagePhase $Branch
  $state = [pscustomobject]@{
    version = 1
    kind = $Kind
    reference = $Reference
    rootSha = $RootSha.ToLowerInvariant()
    branch = $Branch
    repairAttempt = 0
    phase = 'monitoring'
    latestSha = $null
    latestRunId = $null
    repairPr = $null
    phaseNumber = $stagePhase
    stageAuthorizedSha = $null
    stageRunId = $null
    upgradeResult = $null
    cleanInstallResult = $null
    imageDigest = $null
    acceptanceRunUrl = $null
    prNumber = $null
    sourceIssue = $SourceIssue
    repairBaseSha = $null
    deploymentSha = $null
    terminalReason = $null
    updatedAt = $null
  }
  Save-LoopState $state | Out-Null
  return $state
}

function Ensure-LoopDependencies {
  Invoke-Native -File 'pnpm' -Arguments @('install', '--frozen-lockfile') -WorkingDirectory $controlledClone | Out-Null
}

function Ensure-ControlledClone {
  if (-not (Test-Path -LiteralPath (Join-Path $controlledClone '.git'))) {
    $origin = (Invoke-Native -File 'git' -Arguments @('remote', 'get-url', 'origin') -WorkingDirectory $repositoryRoot).Trim()
    Invoke-Native -File 'git' -Arguments @('clone', '--origin', 'origin', $origin, $controlledClone) | Out-Null
  }
  Invoke-Native -File 'git' -Arguments @('fetch', '--prune', 'origin') -WorkingDirectory $controlledClone | Out-Null
}

function Set-ControlledBranch {
  param([string]$Branch, [string]$StartPoint)
  Ensure-ControlledClone
  Invoke-Native -File 'git' -Arguments @('switch', '--discard-changes', '--force-create', $Branch, $StartPoint) -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('clean', '-ffd') -WorkingDirectory $controlledClone | Out-Null
}

function Get-WorkflowRun {
  param([string]$Workflow, [string]$Sha, [string]$Event)
  $runs = Invoke-GhJson @('run', 'list', '--workflow', $Workflow, '--limit', '50', '--json', 'databaseId,status,conclusion,headSha,event,url,createdAt')
  return @($runs | Where-Object { $_.headSha -eq $Sha -and $_.event -eq $Event } | Sort-Object createdAt -Descending | Select-Object -First 1)[0]
}

function Get-NextAttempt {
  param([pscustomobject]$State)
  if ($State.repairAttempt -ge 3 -or $State.phase -eq 'blocked') {
    return $null
  }
  return ([int]$State.repairAttempt + 1)
}

function Set-Blocked {
  param([pscustomobject]$State, [string]$Reason, [string]$RunUrl)
  $State.phase = 'blocked'
  $State.terminalReason = $Reason
  Save-LoopState $State | Out-Null
  $title = "Loop 已阻塞: $($State.rootSha.Substring(0, 12))"
  $body = "本机 Codex CLI 已停止自动写入。`n`n原因：$Reason`n`n根提交：$($State.rootSha)`n修复次数：$($State.repairAttempt)`n运行：$RunUrl"
  $existing = Invoke-GhJson @('issue', 'list', '--state', 'open', '--search', "`"$title`" in:title", '--json', 'number')
  if ($existing.Count -gt 0) {
    Invoke-Native -File 'gh' -Arguments @('issue', 'comment', "$($existing[0].number)", '--body', $body) | Out-Null
  } else {
    Invoke-Native -File 'gh' -Arguments @('issue', 'create', '--title', $title, '--body', $body, '--label', 'agent:blocked') | Out-Null
  }
}

function Download-Diagnostics {
  param([pscustomobject]$State, [pscustomobject]$Run, [string]$Pattern)
  $destination = Join-Path $reportDirectory "$($State.rootSha)-$($Run.databaseId)"
  New-Item -ItemType Directory -Force -Path $destination | Out-Null
  Invoke-Native -File 'gh' -Arguments @('run', 'view', "$($Run.databaseId)", '--log-failed') | Set-Content -LiteralPath (Join-Path $destination 'failed.log') -Encoding utf8
  try {
    Invoke-Native -File 'gh' -Arguments @('run', 'download', "$($Run.databaseId)", '--pattern', $Pattern, '--dir', $destination) | Out-Null
  } catch {
    $_ | Out-String | Set-Content -LiteralPath (Join-Path $destination 'artifact-download-error.txt') -Encoding utf8
  }
  return $destination
}

function Read-StageEvidence {
  param([pscustomobject]$State, [pscustomobject]$Run)
  $destination = Download-Diagnostics -State $State -Run $Run -Pattern 'stage-evidence'
  $evidenceFile = Get-ChildItem -LiteralPath $destination -Recurse -File -Filter 'stage-acceptance.json' | Select-Object -First 1
  if (-not $evidenceFile) {
    throw "Stage run $($Run.databaseId) did not contain stage-acceptance.json."
  }
  $evidence = Get-Content -Raw -LiteralPath $evidenceFile.FullName | ConvertFrom-Json
  if ($evidence.candidateSha -ne $State.latestSha -or [int]$evidence.phase -ne [int]$State.phaseNumber) {
    throw 'Stage evidence does not match the current phase and PR Head SHA.'
  }
  if ($evidence.upgrade -ne 'success' -or $evidence.cleanInstall -ne 'success') {
    throw 'Stage evidence does not prove both upgrade and clean-install success.'
  }
  return $evidence
}

function Get-NonRepairableStageReason {
  param([string]$DiagnosticDirectory, [string]$Conclusion)
  if ($Conclusion -in @('action_required', 'startup_failure', 'stale', 'cancelled', 'timed_out')) {
    return "阶段验收基础设施状态为 $Conclusion，自动修复已停止。"
  }
  $patterns = @(
    'Automatic state restoration is disabled',
    'No previous health record exists',
    'without-safe-restoration',
    'upgrade-failed-without-previous-health-record',
    'upgrade-failed-after-migration-change',
    'VM-local Compose environment file is missing',
    'Stage VM resources are insufficient',
    'Resource not accessible by integration',
    'credentials are unavailable'
  )
  $files = @(Get-ChildItem -LiteralPath $DiagnosticDirectory -Recurse -File -ErrorAction SilentlyContinue)
  foreach ($pattern in $patterns) {
    if ($files.Count -gt 0 -and (Select-String -LiteralPath $files.FullName -SimpleMatch $pattern -Quiet)) {
      return "阶段验收遇到不可安全自动修复的问题：$pattern"
    }
  }
  return $null
}

function Test-LoopChangeBoundary {
  Invoke-Native -File 'git' -Arguments @('diff', '--check') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'node' -Arguments @('scripts/loop/validate-loop-change.mjs') -WorkingDirectory $controlledClone | Out-Null
}

function Invoke-ChangedScopeVerification {
  $files = @(Invoke-Native -File 'git' -Arguments @('diff', '--name-only', '--') -WorkingDirectory $controlledClone -split "`r?`n" | Where-Object { $_ })
  if ($files | Where-Object { $_ -match '^(apps/|packages/openapi/|packages/ui-tokens/)' }) {
    Ensure-LoopDependencies
    Invoke-Native -File 'pnpm' -Arguments @('contract:lint') -WorkingDirectory $controlledClone | Out-Null
    Invoke-Native -File 'pnpm' -Arguments @('typecheck') -WorkingDirectory $controlledClone | Out-Null
    Invoke-Native -File 'pnpm' -Arguments @('lint') -WorkingDirectory $controlledClone | Out-Null
    Invoke-Native -File 'pnpm' -Arguments @('test') -WorkingDirectory $controlledClone | Out-Null
    Invoke-Native -File 'pnpm' -Arguments @('build') -WorkingDirectory $controlledClone | Out-Null
  }
  if ($files | Where-Object { $_ -match '^(services/|packages/proto/|pom\.xml|platform/docker/)' }) {
    Invoke-Native -File (Join-Path $controlledClone 'mvnw.cmd') -Arguments @('-B', '-ntp', 'verify') -WorkingDirectory $controlledClone | Out-Null
  }
}

function Invoke-CodexChange {
  param([pscustomobject]$State, [string]$DiagnosticDirectory, [string]$Mode)
  $attempt = Get-NextAttempt $State
  if ($null -eq $attempt) {
    Set-Blocked $State '已达到 3 次本机 Codex 修复上限。' ''
    return $false
  }

  $startPoint = if ($Mode -eq 'deployment') { 'origin/master' } else { "origin/$($State.branch)" }
  Set-ControlledBranch -Branch $State.branch -StartPoint $startPoint
  $State.repairBaseSha = (Invoke-Native -File 'git' -Arguments @('rev-parse', 'HEAD') -WorkingDirectory $controlledClone).Trim()
  Save-LoopState $State | Out-Null
  $report = Join-Path $DiagnosticDirectory "codex-attempt-$attempt.md"
  $policy = Get-Content -Raw (Join-Path $repositoryRoot 'scripts\loop\prompts\repair.md')
  $prompt = "$policy`n`n失败类别：$Mode`n诊断目录：$DiagnosticDirectory"

  try {
    Invoke-Native -File 'codex' -Arguments @('exec', '--ephemeral', '-C', $controlledClone, '--sandbox', 'workspace-write', '--approve-for-me', '--output-last-message', $report, $prompt) | Out-Null
    Test-LoopChangeBoundary
    Invoke-ChangedScopeVerification
    return $true
  } catch {
    $State.repairAttempt = $attempt
    $State.phase = 'monitoring'
    Save-LoopState $State | Out-Null
    if ($attempt -ge 3) {
      Set-Blocked $State "第 $attempt 次修复未通过本地边界或验证：$($_.Exception.Message)" ''
    }
    return $false
  }
}

function Commit-And-PushRepair {
  param([pscustomobject]$State, [string]$Mode)
  $attempt = Get-NextAttempt $State
  $scope = if ($Mode -eq 'deployment') { '部署' } else { '交付' }
  $loopMode = if ($Mode -eq 'deployment') { 'deploy-repair' } else { 'ci-repair' }
  $messagePath = Join-Path $loopRoot 'commit-message.txt'
  @"
fix($scope): 本机 Codex 修复第 $attempt 轮

功能明细：
- 根据最新 GitHub 失败诊断完成最小修复
- 修复由本机 Codex CLI 在隔离工作树生成

验证结果：
- Loop 修复边界校验：通过
- 受影响本地验证：通过

未运行项：
- GitHub Actions 验证、部署和浏览器联调待执行

阶段状态：
- 阶段 3：待验收

Streamora-Loop-Id: local-$($State.rootSha.Substring(0, 12))
Streamora-Loop-Root: $($State.rootSha)
Streamora-Loop-Attempt: $attempt
Streamora-Loop-Mode: $loopMode
"@ | Set-Content -LiteralPath $messagePath -Encoding utf8
  Invoke-Native -File 'git' -Arguments @('config', 'user.name', 'streamora-local-codex') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('config', 'user.email', 'streamora-local-codex@users.noreply.github.com') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('add', '-A') -WorkingDirectory $controlledClone | Out-Null
  Ensure-LoopDependencies
  Invoke-Native -File 'pnpm' -Arguments @('git:metadata', '--commit-file', $messagePath, '--require-loop-trailers') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('commit', '-F', $messagePath) -WorkingDirectory $controlledClone | Out-Null
  $remoteRef = if ($Mode -eq 'deployment') { 'refs/heads/master' } else { "refs/heads/$($State.branch)" }
  $remoteSha = (Invoke-Native -File 'git' -Arguments @('ls-remote', 'origin', $remoteRef) -WorkingDirectory $controlledClone).Split("`t")[0].Trim()
  if ($remoteSha -and $remoteSha -ne $State.repairBaseSha) {
    throw "Refusing to push a repair based on stale $remoteRef ($($State.repairBaseSha) != $remoteSha)."
  }
  Invoke-Native -File 'git' -Arguments @('push', 'origin', "HEAD:$($State.branch)") -WorkingDirectory $controlledClone | Out-Null
  $State.repairAttempt = $attempt
  $State.latestSha = (Invoke-Native -File 'git' -Arguments @('rev-parse', 'HEAD') -WorkingDirectory $controlledClone).Trim()
  $State.stageRunId = $null
  $State.upgradeResult = $null
  $State.cleanInstallResult = $null
  $State.imageDigest = $null
  $State.acceptanceRunUrl = $null
  $State.phase = 'monitoring'
  Save-LoopState $State | Out-Null
}

function Request-AutoMerge {
  param([int]$PrNumber, [string]$HeadSha)
  Invoke-Native -File 'gh' -Arguments @('pr', 'merge', "$PrNumber", '--auto', '--squash', '--delete-branch', '--match-head-commit', $HeadSha) | Out-Null
}

function Monitor-Deployment {
  param([pscustomobject]$State)
  if (-not $State.deploymentSha) {
    return
  }
  $run = Get-WorkflowRun -Workflow '部署 Core' -Sha $State.deploymentSha -Event 'workflow_dispatch'
  if (-not $run) {
    $run = Get-WorkflowRun -Workflow '部署 Core' -Sha $State.deploymentSha -Event 'workflow_run'
  }
  if (-not $run -or $run.status -ne 'completed') {
    return
  }
  if ($run.conclusion -eq 'success') {
    $State.phase = 'completed'
    Save-LoopState $State | Out-Null
    return
  }
  $State.branch = "deploy-repair/$($State.rootSha.Substring(0, 12))-$([int]$State.repairAttempt + 1)"
  $diagnostics = Download-Diagnostics -State $State -Run $run -Pattern 'deploy-diagnostics'
  if (Invoke-CodexChange -State $State -DiagnosticDirectory $diagnostics -Mode 'deployment') {
    Commit-And-PushRepair -State $State -Mode 'deployment'
    $body = Join-Path $diagnostics 'repair-pr.md'
    @"
## 变更目的

修复 Core 部署或浏览器联调失败。

## 功能明细

- 本机 Codex CLI 根据脱敏部署诊断完成最小修复

## 接口、数据与迁移影响

- 以实际 PR 差异为准；禁止数据库迁移

## 验证结果

- 本机 Loop 边界与受影响验证：通过
- GitHub 验证：待执行

## 未运行项

- 虚拟机部署和浏览器联调由 Actions 执行

## 风险与回滚

- 部署失败时沿用既有最后健康镜像回滚策略

## Loop 证据

- 根提交：$($State.rootSha)
- 修复轮次：$($State.repairAttempt)

## 阶段状态

- 阶段 3：待验收
"@ | Set-Content -LiteralPath $body -Encoding utf8
    Invoke-Native -File 'gh' -Arguments @('pr', 'create', '--base', 'master', '--head', $State.branch, '--title', "fix(部署): 本机 Codex 修复第 $($State.repairAttempt) 轮", '--body-file', $body) | Out-Null
    $newPrNumber = (Invoke-Native -File 'gh' -Arguments @('pr', 'list', '--head', $State.branch, '--base', 'master', '--state', 'open', '--json', 'number', '--jq', '.[0].number')).Trim()
    $State.repairPr = [int]$newPrNumber
    $State.prNumber = [int]$newPrNumber
    $State.deploymentSha = $null
    Save-LoopState $State | Out-Null
  }
}

function Monitor-Pr {
  param([pscustomobject]$State)
  $targetPr = if ($State.prNumber) { $State.prNumber } else { $State.reference }
  $prData = Invoke-GhJson @('pr', 'view', "$targetPr", '--json', 'number,state,headRefName,headRefOid,baseRefName,mergeCommit,url,isDraft,labels')
  if ($prData.baseRefName -ne 'master') {
    throw "PR #$targetPr is not targeting master."
  }
  if ($prData.state -eq 'MERGED') {
    if ($State.branch -like 'deploy-repair/*') {
      if (-not $State.deploymentSha) {
        $State.deploymentSha = $prData.mergeCommit.oid
        Invoke-Native -File 'gh' -Arguments @('workflow', 'run', 'deploy-core.yml', '--ref', 'master', '-f', "ref=$($State.deploymentSha)") | Out-Null
        $State.phase = 'await-deploy'
        Save-LoopState $State | Out-Null
      }
      Monitor-Deployment $State
      return
    }
    if ($null -ne $State.phaseNumber) {
      $mergeSha = $prData.mergeCommit.oid
      if (-not $mergeSha) {
        return
      }
      $promotionRun = Get-WorkflowRun -Workflow '阶段晋升关联' -Sha $mergeSha -Event 'pull_request'
      if (-not $promotionRun -or $promotionRun.status -ne 'completed') {
        $State.phase = 'await-stage-promotion'
        Save-LoopState $State | Out-Null
        return
      }
      if ($promotionRun.conclusion -ne 'success') {
        Set-Blocked $State 'Squash 合并源码树与阶段验收候选无法安全关联。' $promotionRun.url
        return
      }
    }
    $State.phase = 'completed'
    Save-LoopState $State | Out-Null
    if ($State.sourceIssue) {
      Invoke-Native -File 'gh' -Arguments @('issue', 'edit', "$($State.sourceIssue)", '--remove-label', 'agent:running', '--add-label', 'agent:done') | Out-Null
    }
    return
  }
  if ($prData.state -ne 'OPEN') {
    Set-Blocked $State "PR #$targetPr 已关闭且未合并。" $prData.url
    return
  }
  $State.branch = $prData.headRefName
  $State.phaseNumber = Get-StagePhase $prData.headRefName
  $State.latestSha = $prData.headRefOid
  Save-LoopState $State | Out-Null
  $run = Get-WorkflowRun -Workflow '验证' -Sha $prData.headRefOid -Event 'pull_request'
  if (-not $run -or $run.status -ne 'completed') {
    return
  }
  if ($run.conclusion -eq 'success') {
    if ($null -ne $State.phaseNumber) {
      $labels = @($prData.labels.name)
      if ($prData.isDraft -or $labels -notcontains 'stage:ready') {
        $State.phase = 'await-stage-authorization'
        Save-LoopState $State | Out-Null
        return
      }
      if ($State.stageAuthorizedSha -and $State.stageAuthorizedSha -ne $prData.headRefOid) {
        if (-not (Test-AuthorizedLoopRepairCommit -State $State -HeadSha $prData.headRefOid)) {
          $State.phase = 'await-stage-authorization'
          Save-LoopState $State | Out-Null
          return
        }
        $staleStageRun = Get-WorkflowRun -Workflow '阶段验收' -Sha $prData.headRefOid -Event 'pull_request'
        Invoke-Native -File 'gh' -Arguments @('pr', 'edit', "$($prData.number)", '--remove-label', 'stage:ready') | Out-Null
        Invoke-Native -File 'gh' -Arguments @('pr', 'edit', "$($prData.number)", '--add-label', 'stage:ready') | Out-Null
        $State.stageAuthorizedSha = $prData.headRefOid
        $State.stageRunId = if ($staleStageRun) { $staleStageRun.databaseId } else { $null }
        $State.phase = 'await-stage-reauthorization'
        Save-LoopState $State | Out-Null
        return
      }
      if (-not $State.stageAuthorizedSha) {
        $State.stageAuthorizedSha = $prData.headRefOid
        Save-LoopState $State | Out-Null
      }
      $stageRun = Get-WorkflowRun -Workflow '阶段验收' -Sha $prData.headRefOid -Event 'pull_request'
      if ($State.phase -eq 'await-stage-reauthorization' -and $stageRun -and $State.stageRunId -eq $stageRun.databaseId) {
        return
      }
      if (-not $stageRun -or $stageRun.status -ne 'completed') {
        return
      }
      if ($stageRun.conclusion -eq 'success') {
        $evidence = Read-StageEvidence -State $State -Run $stageRun
        $State.stageRunId = $stageRun.databaseId
        $State.upgradeResult = $evidence.upgrade
        $State.cleanInstallResult = $evidence.cleanInstall
        $State.imageDigest = $evidence.imageDigest
        $State.acceptanceRunUrl = $stageRun.url
        Request-AutoMerge -PrNumber $prData.number -HeadSha $prData.headRefOid
        $State.phase = 'await-merge'
        Save-LoopState $State | Out-Null
        return
      }
      $stageVmConclusion = (Invoke-Native -File 'gh' -Arguments @('run', 'view', "$($stageRun.databaseId)", '--json', 'jobs', '--jq', '.jobs[] | select(.name == "虚拟机阶段验收") | .conclusion')).Trim()
      if (-not $stageVmConclusion -or $stageVmConclusion -eq 'skipped') {
        $State.stageAuthorizedSha = $null
        $State.phase = 'await-stage-authorization'
        Save-LoopState $State | Out-Null
        return
      }
      $diagnostics = Download-Diagnostics -State $State -Run $stageRun -Pattern 'stage-diagnostics'
      $terminalReason = Get-NonRepairableStageReason -DiagnosticDirectory $diagnostics -Conclusion $stageRun.conclusion
      if ($terminalReason) {
        Set-Blocked $State $terminalReason $stageRun.url
        return
      }
      if (Invoke-CodexChange -State $State -DiagnosticDirectory $diagnostics -Mode 'pr') {
        Commit-And-PushRepair -State $State -Mode 'pr'
      }
      return
    }
    Request-AutoMerge -PrNumber $prData.number -HeadSha $prData.headRefOid
    $State.phase = 'await-merge'
    Save-LoopState $State | Out-Null
    return
  }
  $diagnostics = Download-Diagnostics -State $State -Run $run -Pattern 'verify-*-diagnostics'
  if (Invoke-CodexChange -State $State -DiagnosticDirectory $diagnostics -Mode 'pr') {
    Commit-And-PushRepair -State $State -Mode 'pr'
  }
}

function Start-PrLoop {
  param([int]$Number)
  $prData = Invoke-GhJson @('pr', 'view', "$Number", '--json', 'number,headRefName,baseRefName,commits')
  if ($prData.baseRefName -ne 'master') {
    throw "PR #$Number is not targeting master."
  }
  $state = Get-LoopState 'pr' "$Number"
  if (-not $state) {
    $state = New-LoopState -Kind 'pr' -Reference "$Number" -RootSha $prData.commits[0].oid -Branch $prData.headRefName
  }
  return $state
}

function Start-IssueLoop {
  param([int]$Number, [int]$PhaseNumber = 0, [bool]$FirstPushAuthorized = $false)
  $issue = Invoke-GhJson @('issue', 'view', "$Number", '--json', 'number,title,body,author,labels,state')
  if ($issue.state -ne 'OPEN' -or -not (@($issue.labels.name) -contains 'agent:ready')) {
    throw "Issue #$Number must be open and labeled agent:ready."
  }
  $permission = (Invoke-Native -File 'gh' -Arguments @('api', "repos/$(Get-RepositoryName)/collaborators/$($issue.author.login)/permission", '--jq', '.permission')).Trim()
  if ($permission -notin @('admin', 'maintain', 'write')) {
    throw "Issue #$Number author does not have repository write permission."
  }
  Ensure-ControlledClone
  $rootSha = (Invoke-Native -File 'git' -Arguments @('rev-parse', 'origin/master') -WorkingDirectory $controlledClone).Trim()
  $branch = if ($PhaseNumber -gt 0) {
    "agent/issue-$Number-phase-$PhaseNumber-$($rootSha.Substring(0, 12))"
  } else {
    "agent/issue-$Number-$($rootSha.Substring(0, 12))"
  }
  $existingState = Get-LoopState 'issue' "$Number"
  if ($existingState) {
    if ($existingState.prNumber) {
      $existingPr = Get-LoopState 'pr' "$($existingState.prNumber)"
      if ($existingPr) {
        return $existingPr
      }
      return (New-LoopState -Kind 'pr' -Reference "$($existingState.prNumber)" -RootSha $existingState.rootSha -Branch $existingState.branch -SourceIssue $Number)
    }
    $existingPrNumber = (Invoke-Native -File 'gh' -Arguments @('pr', 'list', '--head', $existingState.branch, '--base', 'master', '--state', 'open', '--json', 'number', '--jq', '.[0].number')).Trim()
    if ($existingPrNumber) {
      $existingState.prNumber = [int]$existingPrNumber
      $existingState.phase = 'handed-off'
      Save-LoopState $existingState | Out-Null
      return (New-LoopState -Kind 'pr' -Reference "$existingPrNumber" -RootSha $existingState.rootSha -Branch $existingState.branch -SourceIssue $Number)
    }
    throw "Issue #$Number already has loop state without a linked PR. Review $(Get-TaskPath 'issue' "$Number") before retrying."
  }
  if (-not $FirstPushAuthorized) {
    throw "Issue #$Number 的首次推送尚未授权；确认后使用 -AllowFirstPush 重新启动。"
  }
  $state = New-LoopState -Kind 'issue' -Reference "$Number" -RootSha $rootSha -Branch $branch -SourceIssue $Number
  $state.phase = 'running'
  Save-LoopState $state | Out-Null
  Set-ControlledBranch -Branch $branch -StartPoint 'origin/master'
  $diagnostics = Join-Path $reportDirectory "issue-$Number"
  New-Item -ItemType Directory -Force -Path $diagnostics | Out-Null
  @($issue.title, '', $issue.body) | Set-Content -LiteralPath (Join-Path $diagnostics 'issue.md') -Encoding utf8
  $policy = Get-Content -Raw (Join-Path $repositoryRoot 'scripts\loop\prompts\issue.md')
  $prompt = "$policy`n`nIssue 证据文件：$diagnostics\issue.md"
  Invoke-Native -File 'codex' -Arguments @('exec', '--ephemeral', '-C', $controlledClone, '--sandbox', 'workspace-write', '--approve-for-me', '--output-last-message', (Join-Path $diagnostics 'codex.md'), $prompt) | Out-Null
  Test-LoopChangeBoundary
  Invoke-ChangedScopeVerification
  $message = Join-Path $diagnostics 'commit-message.txt'
  @"
feat(交付): 实现 Issue #$Number

功能明细：
- 本机 Codex CLI 实现已授权 Issue

验证结果：
- Loop 修复边界与受影响验证：通过

未运行项：
- GitHub Actions 验证、部署和浏览器联调待执行

阶段状态：
- 阶段 3：待验收

Streamora-Loop-Id: issue-$Number-$($rootSha.Substring(0, 12))
Streamora-Loop-Root: $rootSha
Streamora-Loop-Attempt: 0
Streamora-Loop-Mode: feature
"@ | Set-Content -LiteralPath $message -Encoding utf8
  Invoke-Native -File 'git' -Arguments @('config', 'user.name', 'streamora-local-codex') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('config', 'user.email', 'streamora-local-codex@users.noreply.github.com') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('add', '-A') -WorkingDirectory $controlledClone | Out-Null
  Ensure-LoopDependencies
  Invoke-Native -File 'pnpm' -Arguments @('git:metadata', '--commit-file', $message, '--require-loop-trailers') -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('commit', '-F', $message) -WorkingDirectory $controlledClone | Out-Null
  Invoke-Native -File 'git' -Arguments @('push', '--set-upstream', 'origin', $branch) -WorkingDirectory $controlledClone | Out-Null
  $body = Join-Path $diagnostics 'pr.md'
  @"
## 变更目的

实现已授权 Issue #$Number。

## 功能明细

- 本机 Codex CLI 在隔离工作树中完成最小实现

## 接口、数据与迁移影响

- 以实际 PR 差异为准

## 验证结果

- 本机 Loop 边界与受影响验证：通过
- GitHub 验证：待执行

## 未运行项

- 部署与浏览器联调由 Actions 执行

## 风险与回滚

- 关闭 PR 或 revert squash 合并提交

## Loop 证据

- 根提交：$rootSha

## 阶段状态

- 阶段 3：待验收
"@ | Set-Content -LiteralPath $body -Encoding utf8
  $createPrArguments = @('pr', 'create', '--base', 'master', '--head', $branch, '--title', "feat(交付): 实现 Issue #$Number", '--body-file', $body)
  if ($PhaseNumber -gt 0) {
    $createPrArguments += '--draft'
  }
  Invoke-Native -File 'gh' -Arguments $createPrArguments | Out-Null
  $prNumber = (Invoke-Native -File 'gh' -Arguments @('pr', 'list', '--head', $branch, '--base', 'master', '--state', 'open', '--json', 'number', '--jq', '.[0].number')).Trim()
  Invoke-Native -File 'gh' -Arguments @('issue', 'edit', "$Number", '--remove-label', 'agent:ready', '--add-label', 'agent:running') | Out-Null
  $state.prNumber = [int]$prNumber
  $state.phase = 'handed-off'
  Save-LoopState $state | Out-Null
  return (New-LoopState -Kind 'pr' -Reference "$prNumber" -RootSha $rootSha -Branch $branch -SourceIssue $Number)
}

function Get-WatchedStates {
  return @(Get-ChildItem -LiteralPath $taskDirectory -Filter '*.json' | ForEach-Object { Update-LoopStateSchema (Get-Content -Raw $_.FullName | ConvertFrom-Json) } | Where-Object { $_.kind -ne 'issue' -and $_.phase -notin @('completed', 'blocked', 'handed-off') })
}

function Test-RequestedTaskTerminal {
  param([object[]]$CurrentStates)
  if ($script:LoopWatchMode) {
    return $false
  }
  return @($CurrentStates | Where-Object { $_.phase -notin @('completed', 'blocked', 'handed-off') }).Count -eq 0
}

if (-not $mutex.WaitOne(0)) {
  throw '另一个 Streamora Loop 守护正在运行。'
}

try {
  Invoke-Native -File 'gh' -Arguments @('auth', 'status') | Out-Null
  Invoke-Native -File 'codex' -Arguments @('login', 'status') | Out-Null
  Invoke-Native -File 'gh' -Arguments @('auth', 'setup-git') | Out-Null
  Ensure-LoopLabels
  Ensure-ControlledClone

  $states = switch ($PSCmdlet.ParameterSetName) {
    'Pr' { @(Start-PrLoop $Pr) }
    'Issue' { @(Start-IssueLoop $Issue $StagePhase $AllowFirstPush.IsPresent) }
    default { Get-WatchedStates }
  }

  do {
    foreach ($state in $states) {
      if ($state.phase -notin @('completed', 'blocked')) {
        Monitor-Pr $state
      }
    }
    if ($Once -or (Test-RequestedTaskTerminal $states)) { break }
    Start-Sleep -Seconds $PollSeconds
    $states = Get-WatchedStates
  } while ($true)
} finally {
  $mutex.ReleaseMutex() | Out-Null
  $mutex.Dispose()
}
