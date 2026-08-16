import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateCommitMessage, validatePullRequestBody } from '../validate-git-metadata.mjs';

const directory = path.dirname(fileURLToPath(import.meta.url));
const fixture = (name) => readFileSync(path.join(directory, '..', 'test-fixtures', 'git-metadata', name), 'utf8');

test('接受完整的人工提交中文模板', async () => {
  assert.deepEqual(await validateCommitMessage(fixture('valid-human-commit.txt')), []);
});

test('拒绝不受支持的提交类型和范围', async () => {
  const errors = await validateCommitMessage(fixture('invalid-title-commit.txt'));
  assert.ok(errors.some((error) => error.includes('type must be one of')));
  assert.ok(errors.some((error) => error.includes('scope must be one of')));
});

test('拒绝缺失或空白的提交区块', async () => {
  const errors = await validateCommitMessage(fixture('missing-section-commit.txt'));
  assert.ok(errors.some((error) => error.includes('验证结果')));
  assert.ok(errors.some((error) => error.includes('未运行项')));
});

test('自动化提交必须包含完整且合法的 Loop Trailer', async () => {
  assert.deepEqual(await validateCommitMessage(fixture('valid-automated-commit.txt')), []);
  assert.deepEqual(
    await validateCommitMessage(fixture('valid-automated-commit.txt').replace('Streamora-Loop-Attempt: 1', 'Streamora-Loop-Attempt: 0')),
    []
  );
  const tooManyAttempts = await validateCommitMessage(
    fixture('valid-automated-commit.txt').replace('Streamora-Loop-Attempt: 1', 'Streamora-Loop-Attempt: 6')
  );
  assert.ok(tooManyAttempts.some((error) => error.includes('Streamora-Loop-Attempt')));

  const errors = await validateCommitMessage(fixture('invalid-automated-commit.txt'));
  assert.ok(errors.some((error) => error.includes('Streamora-Loop-Root')));
  assert.ok(errors.some((error) => error.includes('Streamora-Loop-Mode')));
});

test('显式自动化模式拒绝缺失 Trailer 的提交', async () => {
  const errors = await validateCommitMessage(fixture('valid-human-commit.txt'), { requireLoopTrailers: true });
  assert.equal(errors.length, 4);
});

test('接受完整 PR 正文模板', () => {
  assert.deepEqual(validatePullRequestBody(fixture('valid-pr.md')), []);
});

test('拒绝缺失或空白 PR 正文区块', () => {
  const errors = validatePullRequestBody(fixture('invalid-pr.md'));
  assert.ok(errors.some((error) => error.includes('接口、数据与迁移影响')));
  assert.ok(errors.some((error) => error.includes('验证结果')));
});
