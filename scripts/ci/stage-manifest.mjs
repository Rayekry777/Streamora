#!/usr/bin/env node

import { readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const directory = path.dirname(fileURLToPath(import.meta.url));
const manifestPath = path.join(directory, 'stage-manifest.json');

export function validateStage(stage) {
  const errors = [];
  if (!Number.isInteger(stage?.phase) || stage.phase < 0) errors.push('phase 必须是非负整数。');
  for (const field of ['composeServices', 'dependencyContainers', 'backendTests']) {
    if (!Array.isArray(stage?.[field]) || stage[field].length === 0) errors.push(`${field} 必须是非空数组。`);
  }
  if (!stage?.playwrightTestDirectory) errors.push('playwrightTestDirectory 不能为空。');
  if (!stage?.testDataVersion) errors.push('testDataVersion 不能为空。');
  for (const field of ['cpu', 'memoryGb', 'diskGb']) {
    if (!Number.isInteger(stage?.minimumResources?.[field]) || stage.minimumResources[field] < 1) {
      errors.push(`minimumResources.${field} 必须是正整数。`);
    }
  }
  return errors;
}

export function loadStage(phase, manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))) {
  const stage = manifest.stages?.[String(phase)];
  if (!stage) throw new Error(`阶段 ${phase} 没有测试清单。`);
  const errors = validateStage(stage);
  if (errors.length > 0) throw new Error(errors.join(' '));
  return stage;
}

export function missingRequiredTags(stage, testSources) {
  const source = testSources.join('\n');
  return ['@smoke', `@phase-${stage.phase}`].filter((tag) => !source.includes(tag));
}

function run(argv = process.argv.slice(2)) {
  const [command, phaseValue, field] = argv;
  const stage = loadStage(Number(phaseValue));
  if (command === 'get') {
    const value = stage[field];
    if (Array.isArray(value)) console.log(value.join(' '));
    else if (typeof value === 'object') console.log(JSON.stringify(value));
    else console.log(value);
    return 0;
  }
  if (command === 'validate-tests') {
    const testRoot = path.resolve(directory, '..', '..', stage.playwrightTestDirectory);
    const sources = readdirSync(testRoot, { recursive: true })
      .filter((entry) => entry.endsWith('.spec.ts'))
      .map((entry) => readFileSync(path.join(testRoot, entry), 'utf8'));
    const missing = missingRequiredTags(stage, sources);
    if (missing.length > 0) throw new Error(`阶段 ${phaseValue} 缺少 E2E 标签：${missing.join(', ')}`);
    console.log(`阶段 ${phaseValue} 测试清单与 E2E 标签校验通过。`);
    return 0;
  }
  throw new Error('用法：stage-manifest.mjs get <phase> <field> | validate-tests <phase>');
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  Promise.resolve().then(() => run()).then((code) => { process.exitCode = code; }).catch((error) => {
    console.error(`阶段测试清单校验失败：${error.message}`);
    process.exitCode = 1;
  });
}
