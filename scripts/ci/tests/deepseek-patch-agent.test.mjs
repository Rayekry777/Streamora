import assert from 'node:assert/strict';
import test from 'node:test';

import { extractJsonObject, isAllowedRepairPath, normalizePatch, validatePatchPaths } from '../deepseek-patch-agent.mjs';

test('解析 DeepSeek 的文件选择 JSON', () => {
  assert.deepEqual(extractJsonObject('```json\n{"files":["apps/web/src/App.vue"]}\n```'), {
    files: ['apps/web/src/App.vue'],
  });
});

test('拒绝不含 Git diff 的模型输出', () => {
  assert.throws(() => normalizePatch('Here is a fix.'), /unified Git patch/);
});

test('允许源码路径并拒绝高风险路径', () => {
  assert.equal(isAllowedRepairPath('apps/web/src/App.vue'), true);
  assert.equal(isAllowedRepairPath('.github/workflows/verify.yml'), false);
  assert.equal(isAllowedRepairPath('services/identity/db/migration/V1__init.sql'), false);
  assert.equal(isAllowedRepairPath('.env.production'), false);
  assert.equal(isAllowedRepairPath('pnpm-lock.yaml'), false);
  assert.equal(isAllowedRepairPath('scripts/ci/validate-repair-patch.sh'), false);
});

test('拒绝未进入选择集的补丁路径', () => {
  const patch = [
    'diff --git a/apps/web/src/App.vue b/apps/web/src/App.vue',
    'index 1234567..89abcde 100644',
    '--- a/apps/web/src/App.vue',
    '+++ b/apps/web/src/App.vue',
    '@@ -1 +1 @@',
    '-old',
    '+new',
  ].join('\n');

  assert.throws(
    () => validatePatchPaths(patch, new Set(['apps/admin-web/src/App.vue'])),
    /was not selected/,
  );
  assert.doesNotThrow(() => validatePatchPaths(patch, new Set(['apps/web/src/App.vue'])));
});
