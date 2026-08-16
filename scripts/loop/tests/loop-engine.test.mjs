import test from 'node:test';
import assert from 'node:assert/strict';

import {
  MAX_REPAIR_ATTEMPTS,
  classifyWorkflowRun,
  createLoopState,
  handoffIssueState,
  isAllowedLoopPath,
  isWatchableState,
  nextRepairAttempt,
  repairBranchName,
  validateLoopDiff
} from '../loop-engine.mjs';

test('rejects secret, workflow, migration, lock, and traversal paths', () => {
  for (const file of [
    '.env',
    '.github/workflows/verify.yml',
    'services/identity/db/migration/V2__unsafe.sql',
    'pnpm-lock.yaml',
    '../outside.txt'
  ]) {
    assert.equal(isAllowedLoopPath(file), false, file);
  }
  assert.equal(isAllowedLoopPath('services/identity/src/main/java/App.java'), true);
});

test('rejects empty and destructive repair diffs', () => {
  assert.deepEqual(validateLoopDiff({ files: [], diff: '' }), ['修复未产生可提交的文件改动。']);
  assert.equal(
    validateLoopDiff({ files: ['scripts/ci/deploy-core.sh'], diff: '- docker compose down -v' }).length,
    1
  );
});

test('limits every root revision to three repair attempts', () => {
  const state = createLoopState({ rootSha: 'a'.repeat(40), kind: 'deployment', reference: '123' });
  assert.equal(nextRepairAttempt(state), 1);
  state.repairAttempt = MAX_REPAIR_ATTEMPTS;
  assert.equal(nextRepairAttempt(state), null);
  assert.equal(repairBranchName(state, 3), `deploy-repair/${'a'.repeat(12)}-3`);
});

test('does not accept stale or incomplete workflow runs as success', () => {
  assert.equal(classifyWorkflowRun({ headSha: 'b'.repeat(40), status: 'completed', conclusion: 'success' }, 'a'.repeat(40)), 'stale');
  assert.equal(classifyWorkflowRun({ headSha: 'a'.repeat(40), status: 'in_progress', conclusion: null }, 'a'.repeat(40)), 'pending');
  assert.equal(classifyWorkflowRun({ headSha: 'a'.repeat(40), status: 'completed', conclusion: 'failure' }, 'a'.repeat(40)), 'failure');
});

test('hands an Issue state off exactly once and never polls it as a PR', () => {
  const issue = createLoopState({ rootSha: 'a'.repeat(40), kind: 'issue', reference: '42' });
  assert.equal(isWatchableState(issue), false);

  const handedOff = handoffIssueState(issue, 73);
  assert.equal(handedOff.phase, 'handed-off');
  assert.equal(handedOff.prNumber, 73);
  assert.equal(isWatchableState(handedOff), false);
  assert.throws(() => handoffIssueState(handedOff, 0), /positive integer/);
});
