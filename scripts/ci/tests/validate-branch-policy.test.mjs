import test from 'node:test';
import assert from 'node:assert/strict';

import { validateBranchPolicy } from '../validate-branch-policy.mjs';

test('accepts feature, agent, and deployment repair branches targeting master', () => {
  for (const branch of ['feature/用户端-首页', 'agent/branch-governance', 'deploy-repair/abc123-1']) {
    assert.deepEqual(validateBranchPolicy('master', branch), []);
  }
});

test('rejects direct master, legacy codex, empty, and invalid target branches', () => {
  assert.equal(validateBranchPolicy('master', 'master').length, 1);
  assert.equal(validateBranchPolicy('master', 'codex/legacy').length, 1);
  assert.equal(validateBranchPolicy('master', '').length, 1);
  assert.equal(validateBranchPolicy('release', 'feature/用户端-首页').length, 1);
});
