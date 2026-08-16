#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import process from 'node:process';

import { validateLoopDiff } from './loop-engine.mjs';

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8' });
}

const files = git(['diff', '--name-only', '--']).split(/\r?\n/).filter(Boolean);
const diff = git(['diff', '-U0', '--']);
const errors = validateLoopDiff({ files, diff });

if (errors.length > 0) {
  console.error('Loop 修复边界校验失败：');
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exitCode = 1;
} else {
  console.log(`Loop 修复边界校验通过：${files.length} 个文件。`);
}
