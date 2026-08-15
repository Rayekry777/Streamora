import { execFileSync } from 'node:child_process';
import { readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const MAX_DIAGNOSTIC_FILES = 20;
const MAX_DIAGNOSTIC_CHARS = 60_000;
const MAX_SOURCE_FILE_CHARS = 24_000;
const MAX_SOURCE_CHARS = 96_000;
const MAX_PATCH_CHARS = 300_000;
const PROHIBITED_PATH = /(?:^|\/)\.env(?:\.|$)|^\.github\/|(?:^|\/)db\/migration\/|^scripts\/ci\/(?:deepseek-patch-agent|validate-repair-patch)\.(?:mjs|sh)$|application-secret.*\.(?:ya?ml)$|\.(?:pem|key|p12|pfx|jks|keystore)$|(?:^|\/)(?:pnpm-lock\.yaml|package-lock\.json|yarn\.lock|composer\.lock|Gemfile\.lock)$/i;

function parseArguments(argv) {
  const values = {};
  for (let index = 0; index < argv.length; index += 2) {
    const key = argv[index];
    const value = argv[index + 1];
    if (!key?.startsWith('--') || !value) {
      throw new Error('Expected --instruction, --diagnostics, --mode, --patch and --report arguments.');
    }
    values[key.slice(2)] = value;
  }
  for (const required of ['instruction', 'diagnostics', 'mode', 'patch', 'report']) {
    if (!values[required]) {
      throw new Error(`Missing required argument: --${required}`);
    }
  }
  return values;
}

function trimTo(text, maximum) {
  return text.length <= maximum ? text : `${text.slice(0, maximum)}\n[truncated]`;
}

function readTextFile(path) {
  return readFileSync(path, 'utf8').replace(/\u0000/g, '');
}

function listedFiles(root) {
  return execFileSync('git', ['ls-files'], { cwd: root, encoding: 'utf8' })
    .split(/\r?\n/)
    .filter(Boolean)
    .filter(isAllowedRepairPath);
}

function collectDiagnosticText(root, directory) {
  const target = resolve(root, directory);
  const files = [];
  const visit = (path) => {
    for (const entry of readdirSync(path, { withFileTypes: true })) {
      const candidate = resolve(path, entry.name);
      if (entry.isDirectory()) {
        visit(candidate);
      } else if (entry.isFile()) {
        files.push(candidate);
      }
    }
  };
  visit(target);

  let size = 0;
  const sections = [];
  for (const file of files.sort().slice(0, MAX_DIAGNOSTIC_FILES)) {
    if (size >= MAX_DIAGNOSTIC_CHARS || statSync(file).size > MAX_DIAGNOSTIC_CHARS) {
      continue;
    }
    const text = trimTo(readTextFile(file), MAX_DIAGNOSTIC_CHARS - size);
    size += text.length;
    sections.push(`--- ${relative(root, file)} ---\n${text}`);
  }
  return sections.join('\n\n');
}

export function extractJsonObject(content) {
  const normalized = content.trim().replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '');
  const start = normalized.indexOf('{');
  const end = normalized.lastIndexOf('}');
  if (start < 0 || end <= start) {
    throw new Error('DeepSeek did not return a JSON object for source selection.');
  }
  return JSON.parse(normalized.slice(start, end + 1));
}

export function normalizePatch(content) {
  const normalized = content.trim().replace(/^```(?:diff|patch)?\s*/i, '').replace(/\s*```$/, '');
  if (!normalized.startsWith('diff --git ')) {
    throw new Error('DeepSeek did not return a unified Git patch.');
  }
  if (normalized.length > MAX_PATCH_CHARS || normalized.includes('\u0000')) {
    throw new Error('DeepSeek returned an invalid patch payload.');
  }
  return `${normalized}\n`;
}

export function isAllowedRepairPath(file) {
  const normalized = file.replaceAll('\\', '/').replace(/^\.\//, '');
  return Boolean(normalized) && !normalized.startsWith('../') && !normalized.includes('/../') && !PROHIBITED_PATH.test(normalized);
}

export function validatePatchPaths(patch, selectedFiles) {
  const matches = [...patch.matchAll(/^diff --git a\/(.+?) b\/(.+)$/gm)];
  if (matches.length === 0) {
    throw new Error('DeepSeek patch does not contain changed paths.');
  }
  for (const [, before, after] of matches) {
    if (!isAllowedRepairPath(before) || !isAllowedRepairPath(after)) {
      throw new Error(`DeepSeek patch changes a prohibited path: ${before} or ${after}`);
    }
    if (!selectedFiles.has(before) || !selectedFiles.has(after)) {
      throw new Error(`DeepSeek patch changes a file that was not selected: ${before} or ${after}`);
    }
  }
}

async function requestDeepSeek({ apiKey, model, messages }) {
  const baseUrl = (process.env.DEEPSEEK_API_BASE_URL ?? 'https://api.deepseek.com').replace(/\/$/, '');
  if (!baseUrl.startsWith('https://')) {
    throw new Error('DEEPSEEK_API_BASE_URL must use HTTPS.');
  }
  const response = await fetch(`${baseUrl}/chat/completions`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ model, messages, stream: false }),
    signal: AbortSignal.timeout(240_000),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || typeof payload?.choices?.[0]?.message?.content !== 'string') {
    throw new Error(`DeepSeek request failed with HTTP ${response.status}.`);
  }
  return payload.choices[0].message.content;
}

async function main() {
  const args = parseArguments(process.argv.slice(2));
  const apiKey = process.env.DEEPSEEK_API_KEY;
  if (!apiKey) {
    throw new Error('DEEPSEEK_API_KEY is not configured.');
  }

  const root = process.cwd();
  const instruction = readTextFile(resolve(root, args.instruction));
  const diagnostics = collectDiagnosticText(root, args.diagnostics);
  const candidates = listedFiles(root);
  const model = process.env.DEEPSEEK_REPAIR_MODEL ?? 'deepseek-chat';

  const selection = await requestDeepSeek({
    apiKey,
    model,
    messages: [
      {
        role: 'system',
        content: 'You select source files for a constrained automated repair. Return only JSON: {"files":["path"]}. Select at most 8 tracked files. Never select secrets, workflow files, database migrations, dependency locks, or files outside the supplied index.',
      },
      {
        role: 'user',
        content: `Repair mode: ${args.mode}\n\nRules:\n${instruction}\n\nDiagnostics:\n${diagnostics}\n\nAllowed tracked file index:\n${candidates.join('\n')}`,
      },
    ],
  });
  const requestedFiles = extractJsonObject(selection).files;
  if (!Array.isArray(requestedFiles) || requestedFiles.length === 0 || requestedFiles.length > 8) {
    throw new Error('DeepSeek source selection must contain one to eight files.');
  }

  const candidateSet = new Set(candidates);
  let sourceSize = 0;
  const sourceSections = [];
  for (const file of requestedFiles) {
    if (typeof file !== 'string' || !candidateSet.has(file)) {
      throw new Error(`DeepSeek selected an unavailable file: ${String(file)}`);
    }
    const text = trimTo(readTextFile(resolve(root, file)), Math.min(MAX_SOURCE_FILE_CHARS, MAX_SOURCE_CHARS - sourceSize));
    sourceSize += text.length;
    sourceSections.push(`--- ${file} ---\n${text}`);
    if (sourceSize >= MAX_SOURCE_CHARS) {
      break;
    }
  }

  const patchResponse = await requestDeepSeek({
    apiKey,
    model,
    messages: [
      {
        role: 'system',
        content: 'You generate the smallest safe repair as a unified Git diff. Return only a patch beginning with "diff --git". Do not explain. Do not change secrets, GitHub workflows, database migrations, dependency locks, persistent-volume deletion logic, or security controls.',
      },
      {
        role: 'user',
        content: `Repair mode: ${args.mode}\n\nRules:\n${instruction}\n\nDiagnostics:\n${diagnostics}\n\nSelected source files:\n${sourceSections.join('\n\n')}`,
      },
    ],
  });
  const patch = normalizePatch(patchResponse);
  validatePatchPaths(patch, new Set(requestedFiles));
  writeFileSync(resolve(root, args.patch), patch, 'utf8');
  writeFileSync(
    resolve(root, args.report),
    `# DeepSeek repair report\n\n- Mode: ${args.mode}\n- Model: ${model}\n- Selected files:\n${requestedFiles.map((file) => `  - ${file}`).join('\n')}\n- Patch: ${args.patch}\n`,
    'utf8',
  );
}

const invokedPath = process.argv[1] ? resolve(process.argv[1]) : '';
if (invokedPath === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  });
}
