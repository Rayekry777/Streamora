import { readFileSync } from 'node:fs';

export const MAX_REPAIR_ATTEMPTS = 3;
const STAGE_BRANCH = /^(?:feature\/phase-(\d+)-.+|agent\/.+-phase-(\d+)-.+)$/;

const PROHIBITED_PATH = /^(?:\.github\/|\.env(?:\.|$)|.*\/\.env(?:\.|$)|.*application-secret.*\.ya?ml$|.*\.(?:pem|key|p12|pfx|jks|keystore)$|.*\/db\/migration\/|(?:pnpm-lock\.yaml|package-lock\.json|yarn\.lock|npm-shrinkwrap\.json|composer\.lock|Gemfile\.lock)$)/i;
const VOLUME_DELETION = /(?:docker\s+compose\s+.*\bdown\b.*-v|docker\s+volume\s+rm|rm\s+-rf\s+.*\b(?:data|volume|volumes)\b)/i;

export function normalizePath(file) {
  return file.replaceAll('\\', '/').replace(/^\.\//, '');
}

export function isAllowedLoopPath(file) {
  const normalized = normalizePath(file);
  return Boolean(normalized) && !normalized.startsWith('../') && !normalized.includes('/../') && !PROHIBITED_PATH.test(normalized);
}

export function validateLoopDiff({ files, diff }) {
  if (!Array.isArray(files) || files.length === 0) {
    return ['修复未产生可提交的文件改动。'];
  }
  const errors = files
    .filter((file) => !isAllowedLoopPath(file))
    .map((file) => `修复修改了禁止路径：${file}`);
  if (VOLUME_DELETION.test(diff)) {
    errors.push('修复引入了持久卷或数据目录删除逻辑。');
  }
  return errors;
}

export function createLoopState({ rootSha, kind, reference, branch = null }) {
  if (!/^[0-9a-f]{40}$/i.test(rootSha)) {
    throw new Error('rootSha must be a 40-character SHA.');
  }
  if (!['pr', 'issue', 'deployment'].includes(kind)) {
    throw new Error(`Unsupported loop kind: ${kind}`);
  }
  return {
    version: 1,
    rootSha: rootSha.toLowerCase(),
    kind,
    reference,
    branch,
    repairAttempt: 0,
    phase: 'monitoring',
    latestSha: null,
    latestRunId: null,
    repairPr: null,
    phaseNumber: stagePhaseFromBranch(branch),
    stageAuthorizedSha: null,
    stageRunId: null,
    upgradeResult: null,
    cleanInstallResult: null,
    imageDigest: null,
    acceptanceRunUrl: null,
    terminalReason: null,
    updatedAt: new Date().toISOString()
  };
}

export function stagePhaseFromBranch(branch) {
  const match = branch?.match(STAGE_BRANCH);
  return match ? Number(match[1] ?? match[2]) : null;
}

export function requiresStageAcceptance(branch) {
  return stagePhaseFromBranch(branch) !== null;
}

export function applyStageEvidence(state, evidence, run) {
  if (!evidence || evidence.candidateSha !== state.latestSha) {
    throw new Error('Stage evidence does not belong to the latest PR SHA.');
  }
  if (evidence.phase !== state.phaseNumber || evidence.upgrade !== 'success' || evidence.cleanInstall !== 'success') {
    throw new Error('Stage evidence is incomplete or belongs to another phase.');
  }
  return {
    ...state,
    stageAuthorizedSha: evidence.candidateSha,
    stageRunId: run.databaseId,
    upgradeResult: evidence.upgrade,
    cleanInstallResult: evidence.cleanInstall,
    imageDigest: evidence.imageDigest ?? null,
    acceptanceRunUrl: run.url,
    updatedAt: new Date().toISOString()
  };
}

export function nextRepairAttempt(state) {
  if (state.phase === 'blocked' || state.repairAttempt >= MAX_REPAIR_ATTEMPTS) {
    return null;
  }
  return state.repairAttempt + 1;
}

export function isWatchableState(state) {
  return state.kind !== 'issue' && !['completed', 'blocked', 'handed-off'].includes(state.phase);
}

export function handoffIssueState(state, prNumber) {
  if (state.kind !== 'issue') {
    throw new Error('Only issue states can be handed off to a pull request.');
  }
  if (!Number.isInteger(prNumber) || prNumber < 1) {
    throw new Error('prNumber must be a positive integer.');
  }
  return {
    ...state,
    prNumber,
    phase: 'handed-off',
    updatedAt: new Date().toISOString()
  };
}

export function repairBranchName(state, attempt) {
  const root = state.rootSha.slice(0, 12);
  if (state.kind === 'deployment') {
    return `deploy-repair/${root}-${attempt}`;
  }
  return state.branch;
}

export function classifyWorkflowRun(run, expectedSha) {
  if (!run || run.headSha !== expectedSha) {
    return 'stale';
  }
  if (run.status !== 'completed') {
    return 'pending';
  }
  return run.conclusion === 'success' ? 'success' : 'failure';
}

export function readJsonFile(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}
