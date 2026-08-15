import conventional from '@commitlint/config-conventional';

import { COMMIT_SCOPES, COMMIT_TYPES } from './scripts/ci/governance-policy.mjs';

export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    ...conventional.rules,
    'type-enum': [2, 'always', COMMIT_TYPES],
    'scope-enum': [2, 'always', COMMIT_SCOPES],
    'subject-empty': [2, 'never'],
    'subject-full-stop': [2, 'never', '.']
  }
};
