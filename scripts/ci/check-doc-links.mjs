#!/usr/bin/env node

import { existsSync, readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

export function localMarkdownTargets(source) {
  return [...source.matchAll(/!?\[[^\]]*\]\(([^)]+)\)/g)]
    .map((match) => match[1].trim().replace(/\s+"[^"]*"$/, ''))
    .filter((target) => target && !/^(?:https?:|mailto:|#)/i.test(target));
}

function markdownFiles(root) {
  const docs = path.join(root, 'docs');
  const files = readdirSync(docs, { recursive: true })
    .filter((entry) => entry.endsWith('.md'))
    .map((entry) => path.join(docs, entry));
  const readme = path.join(root, 'README.md');
  if (existsSync(readme)) files.push(readme);
  return files;
}

function run(root = process.cwd()) {
  const errors = [];
  for (const file of markdownFiles(root)) {
    for (const target of localMarkdownTargets(readFileSync(file, 'utf8'))) {
      const pathname = decodeURIComponent(target.split('#')[0].split('?')[0]);
      const resolved = path.resolve(path.dirname(file), pathname);
      if (!existsSync(resolved)) errors.push(`${path.relative(root, file)} -> ${target}`);
    }
  }
  for (const error of errors) console.error(`- 失效本地链接：${error}`);
  return errors.length === 0 ? 0 : 1;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) process.exitCode = run();
