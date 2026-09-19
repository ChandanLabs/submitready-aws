import fs from 'node:fs/promises';
import { validateApplication } from './validator.js';

const file = process.argv[2];
if (!file) {
  console.error('Usage: npm run validate:demo -- demo/application-broken.json');
  process.exit(1);
}

const packet = JSON.parse(await fs.readFile(file, 'utf8'));
const report = validateApplication(packet, { today: '2026-09-18' });
console.log(JSON.stringify(report, null, 2));
