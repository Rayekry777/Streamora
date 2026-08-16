import assert from 'node:assert/strict';
import test from 'node:test';

import { validatePrGovernance } from '../validate-pr-governance.mjs';

test('rejects secrets, destructive executable changes, and mixed business commits', () => {
  const errors = validatePrGovernance({
    files: ['.env.production', 'apps/web/src/App.vue', 'services/video-service/src/App.java'],
    diff: '+++ b/scripts/cleanup.sh\n+docker compose down -v\n',
    commits: [{ sha: 'abc', files: ['apps/web/src/App.vue', 'services/video-service/src/App.java'] }]
  });
  assert.equal(errors.length, 3);
});

test('allows examples and separately scoped frontend and backend commits', () => {
  const errors = validatePrGovernance({
    files: ['platform/compose/.env.example', 'apps/web/src/App.vue', 'services/video-service/src/App.java'],
    diff: '+++ b/docs/runbook.md\n+docker compose down -v\n',
    commits: [
      { sha: 'front', files: ['apps/web/src/App.vue'] },
      { sha: 'back', files: ['services/video-service/src/App.java'] }
    ]
  });
  assert.deepEqual(errors, []);
});

test('allows dangerous command fixtures inside governance test sources', () => {
  const errors = validatePrGovernance({
    files: ['scripts/ci/tests/pr-governance.test.mjs'],
    diff: "+++ b/scripts/ci/tests/pr-governance.test.mjs\n+const fixture = 'docker compose down -v';\n",
    commits: [{ sha: 'test', files: ['scripts/ci/tests/pr-governance.test.mjs'] }]
  });
  assert.deepEqual(errors, []);
});
