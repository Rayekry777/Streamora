#!/usr/bin/env node

import { appendFileSync, readFileSync } from 'node:fs';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const FRONTEND_PATH = /^(?:apps\/(?:web|admin-web)\/|packages\/(?:openapi|ui-tokens)\/|package\.json$|pnpm-(?:lock|workspace)\.yaml$|redocly\.yaml$)/;
const BACKEND_PATH = /^(?:services\/|packages\/proto\/|pom\.xml$|mvnw(?:\.cmd)?$|\.mvn\/)/;
const CONTRACT_PATH = /^packages\/(?:openapi|proto)\//;
const RUNTIME_PATH = /(?:^|\/)(?:Dockerfile|docker-compose[^/]*\.ya?ml)$|^platform\/(?:compose|docker)\//;
const MIGRATION_PATH = /\/db\/migration\//;
const REAL_ADAPTER_PATH = /^services\/[^/]+\/src\/main\/(?:java\/.*\/(?:infrastructure|adapter)\/|resources\/application-compose\.ya?ml$)/;
const DOCUMENTATION_PATH = /^(?:docs\/|README\.md$|[^/]+\.md$|\.agents\/)/;

function normalizePath(file) {
  return file.trim().replaceAll('\\', '/').replace(/^\.\//, '');
}

function backendService(file) {
  return file.match(/^services\/([^/]+)\//)?.[1] ?? null;
}

function frontendService(file) {
  if (file.startsWith('apps/web/')) return 'web';
  if (file.startsWith('apps/admin-web/')) return 'admin-web';
  return null;
}

export function classifyChanges(inputFiles, options = {}) {
  const files = [...new Set(inputFiles.map(normalizePath).filter(Boolean))];
  if (options.all) {
    return {
      files,
      docsOnly: false,
      frontend: true,
      backend: true,
      contract: true,
      runtime: true,
      backendModules: [],
      runtimeServices: []
    };
  }

  const backendModules = new Set();
  const runtimeServices = new Set();
  let frontend = false;
  let backend = false;
  let contract = false;
  let runtime = false;

  for (const file of files) {
    frontend ||= FRONTEND_PATH.test(file);
    backend ||= BACKEND_PATH.test(file);
    contract ||= CONTRACT_PATH.test(file);
    runtime ||= RUNTIME_PATH.test(file) || MIGRATION_PATH.test(file) || REAL_ADAPTER_PATH.test(file);

    const service = backendService(file);
    if (service) {
      backendModules.add(`services/${service}`);
      if (RUNTIME_PATH.test(file) || MIGRATION_PATH.test(file) || REAL_ADAPTER_PATH.test(file)) {
        runtimeServices.add(service);
      }
    }
    const web = frontendService(file);
    if (web && RUNTIME_PATH.test(file)) {
      runtimeServices.add(web);
    }
  }

  if (contract) {
    frontend = true;
    backend = true;
  }
  if (backendModules.size > 1) {
    runtime = true;
  }
  if (backendModules.size > 1) {
    for (const module of backendModules) {
      runtimeServices.add(module.replace(/^services\//, ''));
    }
  }

  return {
    files,
    docsOnly: files.length > 0 && files.every((file) => DOCUMENTATION_PATH.test(file)),
    frontend,
    backend,
    contract,
    runtime,
    backendModules: [...backendModules].sort(),
    runtimeServices: [...runtimeServices].sort()
  };
}

function parseArguments(argv) {
  const options = { all: false, fileList: null, githubOutput: null };
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === '--all') options.all = true;
    else if (argument === '--file-list') options.fileList = argv[++index];
    else if (argument === '--github-output') options.githubOutput = argv[++index];
    else throw new Error(`不支持的参数：${argument}`);
  }
  if (!options.all && !options.fileList) {
    throw new Error('必须提供 --file-list 或 --all');
  }
  return options;
}

export function githubOutputs(classification) {
  return {
    docs_only: String(classification.docsOnly),
    frontend: String(classification.frontend),
    backend: String(classification.backend),
    contract: String(classification.contract),
    runtime: String(classification.runtime),
    backend_modules: classification.backendModules.join(','),
    runtime_services: classification.runtimeServices.join(',')
  };
}

function run(argv = process.argv.slice(2)) {
  const options = parseArguments(argv);
  const files = options.fileList ? readFileSync(options.fileList, 'utf8').split(/\r?\n/) : [];
  const result = classifyChanges(files, { all: options.all });
  const outputs = githubOutputs(result);
  const output = Object.entries(outputs).map(([key, value]) => `${key}=${value}`).join('\n');
  if (options.githubOutput) {
    appendFileSync(options.githubOutput, `${output}\n`, 'utf8');
  } else {
    console.log(JSON.stringify(result, null, 2));
  }
}

const isMainModule = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (isMainModule) {
  Promise.resolve().then(() => run()).catch((error) => {
    console.error(`改动分类失败：${error.message}`);
    process.exitCode = 1;
  });
}
