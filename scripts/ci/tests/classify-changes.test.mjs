import assert from 'node:assert/strict';
import test from 'node:test';

import { classifyChanges, githubOutputs } from '../classify-changes.mjs';

test('pure documentation changes skip functional jobs', () => {
  const result = classifyChanges(['docs/acceptance/PHASE_3_ACCEPTANCE.md']);
  assert.equal(result.docsOnly, true);
  assert.equal(result.frontend, false);
  assert.equal(result.backend, false);
  assert.equal(result.docker, false);
});

test('contracts validate both consumers and protobuf activates Docker integration', () => {
  const openapi = classifyChanges(['packages/openapi/streamora-v1.yaml']);
  assert.equal(openapi.frontend, true);
  assert.equal(openapi.backend, true);
  assert.equal(openapi.contract, true);

  const protobuf = classifyChanges(['packages/proto/src/main/proto/media.proto']);
  assert.equal(protobuf.docker, true);
});

test('migration changes select the owning backend and Docker service', () => {
  const result = classifyChanges(['services/media-service/src/main/resources/db/migration/V2__media.sql']);
  assert.deepEqual(result.backendModules, ['services/media-service']);
  assert.deepEqual(result.dockerServices, ['media-service']);
  assert.equal(result.docker, true);
  assert.equal(githubOutputs(result).backend_modules, 'services/media-service');
});

test('multiple files in one service remain a lightweight backend change', () => {
  const result = classifyChanges([
    'services/video-service/src/main/java/App.java',
    'services/video-service/src/test/java/AppTest.java'
  ]);
  assert.equal(result.backend, true);
  assert.equal(result.docker, false);
});

test('cross-service changes boot the affected services against real dependencies', () => {
  const result = classifyChanges([
    'services/media-service/src/main/java/MediaApplication.java',
    'services/transcode-worker/src/main/java/TranscodeApplication.java'
  ]);
  assert.equal(result.docker, true);
  assert.deepEqual(result.dockerServices, ['media-service', 'transcode-worker']);
});

test('real adapter changes boot their owning service', () => {
  const result = classifyChanges([
    'services/media-service/src/main/java/com/streamora/media/infrastructure/S3ObjectStore.java'
  ]);
  assert.equal(result.docker, true);
  assert.deepEqual(result.dockerServices, ['media-service']);
});
