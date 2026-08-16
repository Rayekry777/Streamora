import assert from 'node:assert/strict';
import test from 'node:test';

import { classifyChanges, githubOutputs } from '../classify-changes.mjs';

test('pure documentation changes skip functional jobs', () => {
  const result = classifyChanges(['docs/acceptance/PHASE_3_ACCEPTANCE.md']);
  assert.equal(result.docsOnly, true);
  assert.equal(result.frontend, false);
  assert.equal(result.backend, false);
  assert.equal(result.runtime, false);
});

test('contracts validate both consumers without automatically starting Docker smoke', () => {
  const openapi = classifyChanges(['packages/openapi/streamora-v1.yaml']);
  assert.equal(openapi.frontend, true);
  assert.equal(openapi.backend, true);
  assert.equal(openapi.contract, true);

  const protobuf = classifyChanges(['packages/proto/src/main/proto/media.proto']);
  assert.equal(protobuf.frontend, true);
  assert.equal(protobuf.backend, true);
  assert.equal(protobuf.runtime, false);
});

test('migration changes select the owning backend and runtime service', () => {
  const result = classifyChanges(['services/media-service/src/main/resources/db/migration/V2__media.sql']);
  assert.deepEqual(result.backendModules, ['services/media-service']);
  assert.deepEqual(result.runtimeServices, ['media-service']);
  assert.equal(result.runtime, true);
  assert.equal(githubOutputs(result).backend_modules, 'services/media-service');
  assert.equal(githubOutputs(result).runtime_services, 'media-service');
});

test('multiple files in one service remain a lightweight backend change', () => {
  const result = classifyChanges([
    'services/video-service/src/main/java/App.java',
    'services/video-service/src/test/java/AppTest.java'
  ]);
  assert.equal(result.backend, true);
  assert.equal(result.runtime, false);
});

test('cross-service changes boot the affected services against real dependencies', () => {
  const result = classifyChanges([
    'services/media-service/src/main/java/MediaApplication.java',
    'services/transcode-worker/src/main/java/TranscodeApplication.java'
  ]);
  assert.equal(result.runtime, true);
  assert.deepEqual(result.runtimeServices, ['media-service', 'transcode-worker']);
});

test('real adapter changes boot their owning service', () => {
  const result = classifyChanges([
    'services/media-service/src/main/java/com/streamora/media/infrastructure/S3ObjectStore.java'
  ]);
  assert.equal(result.runtime, true);
  assert.deepEqual(result.runtimeServices, ['media-service']);
});

test('workflow, CI, stage manifest, and test changes never start automatic Docker smoke', () => {
  for (const file of [
    '.github/workflows/verify.yml',
    'scripts/ci/run-functional-docker-tests.sh',
    'scripts/ci/stage-manifest.json',
    'tests/e2e/media-flow.spec.ts'
  ]) {
    assert.equal(classifyChanges([file]).runtime, false, file);
  }
});

test('Compose and Dockerfile changes start runtime smoke', () => {
  assert.equal(classifyChanges(['platform/compose/compose.yml']).runtime, true);
  assert.equal(classifyChanges(['services/media-service/Dockerfile']).runtime, true);
});
