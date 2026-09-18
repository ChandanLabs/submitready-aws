import test from 'node:test';
import assert from 'node:assert/strict';
import { validateApplication } from '../src/validator.js';
import fs from 'node:fs';

const broken = JSON.parse(fs.readFileSync(new URL('../demo/application-broken.json', import.meta.url)));
const fixed = JSON.parse(fs.readFileSync(new URL('../demo/application-fixed.json', import.meta.url)));

test('broken packet produces actionable failures', () => {
  const report = validateApplication(broken, { today: '2026-09-18' });
  assert.equal(report.ready, false);
  assert.ok(report.results.find(r => r.id === 'name-match' && !r.passed));
  assert.ok(report.results.find(r => r.id === 'income-match' && !r.passed));
});

test('fixed packet passes all checks', () => {
  const report = validateApplication(fixed, { today: '2026-09-18' });
  assert.equal(report.ready, true);
  assert.equal(report.passedCount, report.totalChecks);
});
