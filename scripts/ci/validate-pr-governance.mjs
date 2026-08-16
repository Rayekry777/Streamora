#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const SECRET_PATH = /(^|\/)(?:\.env(?:\..*)?|secrets?\/|[^/]*\.(?:pem|key|p12|pfx|jks|keystore)$)|application-secret.*\.ya?ml$/i;
const SAFE_ENV_EXAMPLE = /\.env(?:\.[^/]*)?\.example$/;
const FRONTEND_PATH = /^(?:apps\/(?:web|admin-web)\/|packages\/ui-tokens\/)/;
const BACKEND_PATH = /^(?:services\/|packages\/proto\/|pom\.xml$)/;
const EXECUTABLE_PATH = /^(?:scripts\/|\.github\/workflows\/|platform\/)/;
const DANGEROUS_ADDITION = /(?:docker\s+compose\s+[^\n]*\bdown\b[^\n]*(?:\s-v\b|--volumes\b)|docker\s+volume\s+rm\b|rm\s+-rf\s+[^\n]*(?:data|volume|volumes))/i;

export function validatePrGovernance({ files, diff, commits }) {
  const errors = [];
  for (const file of files) {
    if (SECRET_PATH.test(file) && !SAFE_ENV_EXAMPLE.test(file)) errors.push(`禁止提交敏感路径：${file}`);
  }
  const executableAdditions = diff.split(/\r?\n/)
    .filter((line) => line.startsWith('+++ b/') || line.startsWith('+'));
  let currentFile = '';
  const addedByExecutable = [];
  for (const line of executableAdditions) {
    if (line.startsWith('+++ b/')) currentFile = line.slice(6);
    else if (EXECUTABLE_PATH.test(currentFile)) addedByExecutable.push(line.slice(1));
  }
  if (DANGEROUS_ADDITION.test(addedByExecutable.join('\n'))) errors.push('可执行变更新增了持久卷删除或递归数据删除命令。');

  for (const commit of commits) {
    const hasFrontend = commit.files.some((file) => FRONTEND_PATH.test(file));
    const hasBackend = commit.files.some((file) => BACKEND_PATH.test(file));
    if (hasFrontend && hasBackend) errors.push(`提交 ${commit.sha} 同时包含前端与后端业务改动，请拆分提交。`);
  }
  return errors;
}

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8' });
}

function run([base, head = 'HEAD']) {
  if (!base) throw new Error('用法：validate-pr-governance.mjs <base> [head]');
  const files = git(['diff', '--name-only', base, head]).split(/\r?\n/).filter(Boolean);
  const diff = git(['diff', '--unified=0', base, head, '--']);
  const shas = git(['rev-list', '--reverse', `${base}..${head}`]).split(/\r?\n/).filter(Boolean);
  const commits = shas.map((sha) => ({
    sha,
    files: git(['diff-tree', '--no-commit-id', '--name-only', '-r', sha]).split(/\r?\n/).filter(Boolean)
  }));
  const errors = validatePrGovernance({ files, diff, commits });
  for (const error of errors) console.error(`- ${error}`);
  return errors.length === 0 ? 0 : 1;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  try {
    process.exitCode = run(process.argv.slice(2));
  } catch (error) {
    console.error(`PR 治理校验失败：${error.message}`);
    process.exitCode = 1;
  }
}
