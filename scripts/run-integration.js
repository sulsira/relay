// Launches the installed VS Code with the extension and runs out/integration/index.js inside it.
// Usage: npm run test:integration   (set RELAY_WORKSPACE to override the opened folder)
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const workspace = process.env.RELAY_WORKSPACE || path.join(root, 'test-fixtures', 'workspace');
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'relay-it-'));
const resultFile = path.join(scratch, 'result.txt');

const args = [
  '--user-data-dir', path.join(scratch, 'user'),
  '--extensions-dir', path.join(scratch, 'ext'),
  '--extensionDevelopmentPath', root,
  '--extensionTestsPath', path.join(root, 'out', 'integration', 'index.js'),
  '--disable-workspace-trust',
  '--wait',
  workspace,
];
const result = spawnSync('code', args, { stdio: 'inherit', env: { ...process.env, RELAY_TEST_RESULT: resultFile } });

const report = fs.existsSync(resultFile) ? fs.readFileSync(resultFile, 'utf8') : 'FAIL\nno result file written\n';
console.log(report);
fs.rmSync(scratch, { recursive: true, force: true });
process.exit(report.startsWith('PASS') && result.status === 0 ? 0 : 1);
