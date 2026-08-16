#!/usr/bin/env node

import process from 'node:process';
import { fileURLToPath } from 'node:url';

const ALLOWED_HEAD_BRANCH = /^(?:feature|agent|deploy-repair)\/.+$/;

export function validateBranchPolicy(baseRef, headBranch) {
  const errors = [];
  if (baseRef !== 'master') {
    errors.push(`PR 目标分支必须为 master，当前为：${baseRef || '<空>'}`);
  }
  if (!ALLOWED_HEAD_BRANCH.test(headBranch ?? '')) {
    errors.push(`PR 来源分支必须匹配 feature/*、agent/* 或 deploy-repair/*，当前为：${headBranch || '<空>'}`);
  }
  return errors;
}

export function run(argv = process.argv.slice(2)) {
  const [baseRef, headBranch] = argv;
  const errors = validateBranchPolicy(baseRef, headBranch);
  if (errors.length > 0) {
    console.error('分支策略校验失败：');
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    return 1;
  }
  console.log(`分支策略校验通过：${headBranch} -> ${baseRef}`);
  return 0;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  process.exitCode = run();
}
