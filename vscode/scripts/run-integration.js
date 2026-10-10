// Launches the installed VS Code with the extension and runs out/integration/index.js inside it.
// Usage: npm run test:integration   (set RELAY_WORKSPACE to override the opened folder)
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const workspace = process.env.RELAY_WORKSPACE || path.join(root, 'test-fixtures', 'workspace');
const MAX_LAUNCH_ATTEMPTS = 3;

function launch() {
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
  const report = fs.existsSync(resultFile) ? fs.readFileSync(resultFile, 'utf8') : undefined;
  fs.rmSync(scratch, { recursive: true, force: true });
  return { report, status: result.status, signal: result.signal };
}

// VS Code sometimes exits before the extension host runs the scenario. That is a launch problem, so retry it.
// A written FAIL report is a real test failure and is never retried.
let outcome = launch();
for (let attempt = 2; attempt <= MAX_LAUNCH_ATTEMPTS && outcome.report === undefined; attempt++) {
  console.log(`VS Code exited without a result (status=${outcome.status}, signal=${outcome.signal}). Retry ${attempt}/${MAX_LAUNCH_ATTEMPTS}.`);
  outcome = launch();
}

const report = outcome.report ?? 'FAIL\nno result file written after retries\n';
console.log(report);
process.exit(report.startsWith('PASS') && outcome.status === 0 ? 0 : 1);
