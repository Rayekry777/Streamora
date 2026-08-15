#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import lint from '@commitlint/lint';

import {
  COMMIT_SCOPES,
  COMMIT_TYPES,
  LOOP_TRAILERS,
  REQUIRED_COMMIT_SECTIONS,
  REQUIRED_PR_HEADINGS
} from './governance-policy.mjs';

const TITLE_RULES = {
  'type-enum': [2, 'always', COMMIT_TYPES],
  'scope-enum': [2, 'always', COMMIT_SCOPES],
  'subject-empty': [2, 'never'],
  'header-max-length': [2, 'always', 100]
};

function normalize(value) {
  return value.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n').trim();
}

function messageLines(value) {
  return normalize(value).split('\n');
}

function findHeadingIndexes(lines, headings) {
  return headings.map((heading) => lines.findIndex((line) => line.trim() === heading));
}

function validateOrderedSections(lines, headings, errors, label) {
  const indexes = findHeadingIndexes(lines, headings);

  for (let index = 0; index < headings.length; index += 1) {
    if (indexes[index] === -1) {
      errors.push(`${label}缺少区块“${headings[index]}”`);
    }
  }

  let previousIndex = -1;
  for (const index of indexes) {
    if (index !== -1 && previousIndex >= index) {
      errors.push(`${label}区块必须按规定顺序出现`);
      break;
    }
    if (index !== -1) {
      previousIndex = index;
    }
  }

  for (let index = 0; index < indexes.length; index += 1) {
    if (indexes[index] === -1) {
      continue;
    }
    const start = indexes[index] + 1;
    const end = indexes.slice(index + 1).find((headingIndex) => headingIndex !== -1) ?? lines.length;
    if (!lines.slice(start, end).some((line) => line.trim().length > 0)) {
      errors.push(`${label}区块“${headings[index]}”不能为空`);
    }
  }
}

function parseTrailers(lines) {
  const trailers = new Map();
  for (const line of lines) {
    const match = line.match(/^([A-Za-z0-9-]+):\s*(\S.*?)\s*$/);
    if (match) {
      trailers.set(match[1], match[2]);
    }
  }
  return trailers;
}

export async function validateCommitMessage(message, options = {}) {
  const normalized = normalize(message);
  const errors = [];
  const titleResult = await lint(normalized, TITLE_RULES);
  for (const error of titleResult.errors) {
    errors.push(`提交标题：${error.message}`);
  }

  const lines = messageLines(normalized);
  validateOrderedSections(lines, REQUIRED_COMMIT_SECTIONS, errors, '提交');

  const trailers = parseTrailers(lines);
  const containsLoopTrailer = [...trailers.keys()].some((key) => key.startsWith('Streamora-Loop-'));
  if (options.requireLoopTrailers || containsLoopTrailer) {
    for (const [key, valuePattern] of Object.entries(LOOP_TRAILERS)) {
      const value = trailers.get(key);
      if (!value) {
        errors.push(`自动化提交缺少 Trailer“${key}”`);
      } else if (!valuePattern.test(value)) {
        errors.push(`Trailer“${key}”格式无效`);
      }
    }
  }

  return errors;
}

export function validatePullRequestBody(body) {
  const errors = [];
  validateOrderedSections(messageLines(body), REQUIRED_PR_HEADINGS, errors, 'PR');
  return errors;
}

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8' });
}

function getCommitInputs(from, to) {
  const output = git(['log', '--no-merges', '--format=%H%x1f%B%x1e', `${from}..${to}`]);
  return output
    .split('\x1e')
    .filter((entry) => entry.trim().length > 0)
    .map((entry) => {
      const separator = entry.indexOf('\x1f');
      return {
        sha: entry.slice(0, separator),
        message: entry.slice(separator + 1)
      };
    });
}

function parseArguments(argv) {
  const options = { commitFiles: [], requireLoopTrailers: false };
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === '--commit-file') {
      options.commitFiles.push(argv[++index]);
    } else if (argument === '--from') {
      options.from = argv[++index];
    } else if (argument === '--to') {
      options.to = argv[++index];
    } else if (argument === '--pr-body') {
      options.prBody = argv[++index];
    } else if (argument === '--require-loop-trailers') {
      options.requireLoopTrailers = true;
    } else if (argument === '--help' || argument === '-h') {
      options.help = true;
    } else {
      throw new Error(`不支持的参数：${argument}`);
    }
  }
  if ((options.from && !options.to) || (!options.from && options.to)) {
    throw new Error('--from 和 --to 必须同时提供');
  }
  return options;
}

export async function run(argv = process.argv.slice(2)) {
  const options = parseArguments(argv);
  if (options.help) {
    console.log('用法：node scripts/ci/validate-git-metadata.mjs [--from <base> --to <head>] [--commit-file <path>] [--pr-body <path>] [--require-loop-trailers]');
    return 0;
  }
  if (!options.from && options.commitFiles.length === 0 && !options.prBody) {
    throw new Error('至少提供 --from/--to、--commit-file 或 --pr-body 之一');
  }

  const commitInputs = [];
  if (options.from) {
    commitInputs.push(...getCommitInputs(options.from, options.to));
  }
  for (const file of options.commitFiles) {
    commitInputs.push({ sha: path.basename(file), message: readFileSync(file, 'utf8') });
  }

  const errors = [];
  for (const commit of commitInputs) {
    const commitErrors = await validateCommitMessage(commit.message, options);
    errors.push(...commitErrors.map((error) => `[${commit.sha}] ${error}`));
  }
  if (options.prBody) {
    errors.push(...validatePullRequestBody(readFileSync(options.prBody, 'utf8')));
  }
  if (errors.length > 0) {
    console.error('Git 元数据治理校验失败：');
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    return 1;
  }

  console.log(`Git 元数据治理校验通过：${commitInputs.length} 个提交${options.prBody ? '，PR 正文' : ''}`);
  return 0;
}

const isMainModule = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMainModule) {
  run().then((exitCode) => {
    process.exitCode = exitCode;
  }).catch((error) => {
    console.error(`Git 元数据治理校验无法执行：${error.message}`);
    process.exitCode = 2;
  });
}
