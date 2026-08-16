import assert from 'node:assert/strict';
import test from 'node:test';

import { localMarkdownTargets } from '../check-doc-links.mjs';

test('extracts only local Markdown link targets', () => {
  assert.deepEqual(
    localMarkdownTargets('[local](../file.md#part) [web](https://example.com) ![image](./a.png "preview")'),
    ['../file.md#part', './a.png']
  );
});
