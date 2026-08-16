#!/usr/bin/env node

import { appendFileSync } from 'node:fs';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const PHASE_BRANCH = /^feature\/phase-(\d+)-.+$/;

export function phaseFromBranch(branch) {
  const match = branch?.match(PHASE_BRANCH);
  return match ? Number(match[1]) : null;
}

export function evaluateStagePolicy(input) {
  const phase = phaseFromBranch(input.headBranch);
  if (phase === null) {
    return { isStage: false, authorized: false, phase: null, errors: [] };
  }

  const errors = [];
  if (input.baseRef !== 'master') errors.push('阶段 PR 必须以 master 为目标分支。');
  if (input.draft) errors.push('阶段 PR 仍为 Draft。');
  if (input.behindBy !== 0) errors.push('阶段分支不是最新 master。');
  if (input.verifiedSha !== input.headSha) errors.push('普通验证尚未在当前 Head SHA 上通过。');

  return { isStage: true, authorized: errors.length === 0, phase, errors };
}

function envBoolean(value) {
  return value === 'true';
}

function run() {
  const input = {
    baseRef: process.env.STAGE_BASE_REF ?? '',
    headBranch: process.env.STAGE_HEAD_BRANCH ?? '',
    headSha: process.env.STAGE_HEAD_SHA ?? '',
    draft: envBoolean(process.env.STAGE_DRAFT),
    behindBy: Number(process.env.STAGE_BEHIND_BY ?? '-1'),
    verifiedSha: process.env.STAGE_VERIFIED_SHA ?? ''
  };
  const result = evaluateStagePolicy(input);
  const lines = [
    `is_stage=${result.isStage}`,
    `authorized=${result.authorized}`,
    `phase=${result.phase ?? ''}`,
    `head_sha=${input.headSha}`
  ];
  if (process.env.GITHUB_OUTPUT) appendFileSync(process.env.GITHUB_OUTPUT, `${lines.join('\n')}\n`, 'utf8');
  for (const error of result.errors) console.error(`- ${error}`);
  return result.isStage && !result.authorized ? 1 : 0;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  process.exitCode = run();
}
