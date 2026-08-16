import assert from 'node:assert/strict';
import test from 'node:test';

import { evaluateStagePolicy, phaseFromBranch } from '../stage-policy.mjs';
import { loadStage, missingRequiredTags, validateStage } from '../stage-manifest.mjs';

const authorized = {
  baseRef: 'master',
  headBranch: 'feature/phase-3-media-flow',
  headSha: 'a'.repeat(40),
  draft: false,
  behindBy: 0,
  verifiedSha: 'a'.repeat(40)
};

test('recognizes only human stage branches', () => {
  assert.equal(phaseFromBranch('feature/phase-3-media-flow'), 3);
  assert.equal(phaseFromBranch('agent/loop-42-phase-6-agent-chat'), null);
  assert.equal(phaseFromBranch('feature/user-login'), null);
});

test('automatically authorizes only the current verified stage head', () => {
  assert.deepEqual(evaluateStagePolicy(authorized).errors, []);
  assert.equal(evaluateStagePolicy({ ...authorized, draft: true }).authorized, false);
  assert.equal(evaluateStagePolicy({ ...authorized, behindBy: 1 }).authorized, false);
  assert.equal(evaluateStagePolicy({ ...authorized, verifiedSha: 'b'.repeat(40) }).authorized, false);
});

test('validates the phase 3 manifest and required E2E evidence', () => {
  const stage = loadStage(3);
  assert.deepEqual(validateStage(stage), []);
  assert.deepEqual(missingRequiredTags(stage, ['@smoke @phase-2 @phase-3']), []);
  assert.deepEqual(missingRequiredTags(stage, ['@phase-2']), ['@smoke', '@phase-3']);
});
