import assert from 'node:assert/strict';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import * as vscode from 'vscode';

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

const relayNames = (): string[] =>
  vscode.window.terminals.map((t) => t.name).filter((name) => name.startsWith('Relay:'));

async function waitFor(condition: () => boolean, what: string, timeoutMs = 8000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (condition()) return;
    await sleep(100);
  }
  assert.fail(`timed out waiting for ${what}`);
}


interface CaptureCase {
  agent: { name: string; command: string; referenceMode?: string };
  trigger: 'relay.sendFilePath' | 'relay.sendSelection';
  expected: string;
}

/**
 * Runs `cat > file` as the agent, pastes through Relay, then sends Ctrl+D twice
 * (first flushes the unsubmitted line, second closes cat) and reads what arrived.
 */
async function captureCase(testCase: CaptureCase, document: vscode.TextDocument): Promise<void> {
  const outFile = path.join(os.tmpdir(), `relay-it-${process.pid}-${testCase.agent.name}.txt`);
  fs.rmSync(outFile, { force: true });

  const agent = { ...testCase.agent, command: `${testCase.agent.command} > '${outFile}'` };
  await vscode.workspace.getConfiguration('relay').update('agents', [agent], vscode.ConfigurationTarget.Global);

  await vscode.commands.executeCommand('relay.openAgent', agent.name);
  await waitFor(() => vscode.window.activeTerminal?.name === `Relay:${agent.name}`, `Relay:${agent.name} active`);
  const terminal = vscode.window.activeTerminal;
  assert.ok(terminal, 'active terminal');
  await sleep(1000);

  const editor = await vscode.window.showTextDocument(document);
  editor.selection = new vscode.Selection(2, 0, 4, 0);
  await vscode.commands.executeCommand(testCase.trigger);
  await sleep(400);

  terminal.sendText('\x04', false);
  await sleep(200);
  terminal.sendText('\x04', false);

  await waitFor(() => fs.existsSync(outFile) && fs.readFileSync(outFile, 'utf8').length > 0, `${agent.name} output file`);
  const received = fs.readFileSync(outFile, 'utf8');
  fs.rmSync(outFile, { force: true });
  assert.equal(received, testCase.expected, `${agent.name}: pasted text`);
  terminal.dispose();
}

async function scenario(): Promise<string[]> {
  const log: string[] = [];
  const step = (message: string): void => {
    log.push(message);
  };

  const extension = vscode.extensions.getExtension('sulsira.relay-vscode');
  assert.ok(extension, 'extension is installed');
  await extension.activate();
  step('activated');

  const commands = await vscode.commands.getCommands(true);
  const expected = ['relay.open', 'relay.openAgent', 'relay.sendFilePath', 'relay.sendSelection', 'relay.setDefaultAgent'];
  expected.forEach((id) => assert.ok(commands.includes(id), `command ${id} registered`));
  step('commands registered');

  const config = vscode.workspace.getConfiguration('relay');
  await config.update('agents', [{ name: 'a1', command: 'cat' }, { name: 'a2', command: 'cat', referenceMode: 'add' }], vscode.ConfigurationTarget.Global);
  await config.update('defaultAgent', 'a1', vscode.ConfigurationTarget.Global);
  await config.update('agentStartupDelayMs', 0, vscode.ConfigurationTarget.Global);

  await vscode.commands.executeCommand('relay.open');
  await waitFor(() => relayNames().includes('Relay:a1'), 'Relay:a1 terminal');
  step('relay.open created Relay:a1');

  await vscode.commands.executeCommand('relay.openAgent', 'a2');
  await waitFor(() => relayNames().includes('Relay:a2'), 'Relay:a2 terminal');
  await waitFor(() => vscode.window.activeTerminal?.name === 'Relay:a2', 'Relay:a2 active');
  step('relay.openAgent created and focused Relay:a2');

  await vscode.commands.executeCommand('relay.openAgent', 'a2');
  assert.equal(relayNames().filter((n) => n === 'Relay:a2').length, 1, 'no duplicate a2 terminal');
  step('reopening a2 reuses its terminal');

  const folder = vscode.workspace.workspaceFolders?.[0];
  assert.ok(folder, 'workspace folder is open');
  const file = vscode.Uri.joinPath(folder.uri, 'src/sample.ts');
  const document = await vscode.workspace.openTextDocument(file);
  const editor = await vscode.window.showTextDocument(document);
  editor.selection = new vscode.Selection(2, 0, 4, 0);

  await vscode.commands.executeCommand('relay.sendFilePath');
  await vscode.commands.executeCommand('relay.sendSelection');
  await sleep(500);
  assert.equal(relayNames().length, 2, 'sends reuse existing terminals');
  assert.equal(vscode.window.activeTerminal?.name, 'Relay:a2', 'send targets the last used Relay terminal');
  step('sends reuse the active Relay terminal');

  vscode.window.terminals.find((t) => t.name === 'Relay:a2')?.dispose();
  await waitFor(() => !relayNames().includes('Relay:a2'), 'a2 closed');
  await vscode.commands.executeCommand('relay.sendFilePath');
  await sleep(500);
  assert.deepEqual(relayNames(), ['Relay:a1'], 'after a2 closes, sends fall back to a1');
  step('closing a2 falls back to a1 without a new terminal');

  const ghost = vscode.window.createTerminal({ name: 'Relay:ghost' });
  await waitFor(() => !vscode.window.terminals.includes(ghost), 'stale terminal closed');
  step('stale Relay:* terminal closed');

  const rel = 'src/sample.ts';
  const cases: CaptureCase[] = [
    { agent: { name: 'zed', command: 'cat' }, trigger: 'relay.sendFilePath', expected: `@${rel}` },
    { agent: { name: 'claude', command: 'cat' }, trigger: 'relay.sendSelection', expected: `@${rel}#L3-L5` },
    { agent: { name: 'cursor', command: 'cat' }, trigger: 'relay.sendSelection', expected: `@${rel}:3-5` },
    { agent: { name: 'adder', command: 'cat', referenceMode: 'add' }, trigger: 'relay.sendFilePath', expected: `/add @${rel}` },
  ];
  for (const testCase of cases) {
    await captureCase(testCase, document);
    step(`pasted text verified for ${testCase.agent.name} (${testCase.trigger})`);
  }

  return log;
}

export async function run(): Promise<void> {
  const resultFile = process.env.RELAY_TEST_RESULT;
  try {
    const log = await scenario();
    if (resultFile) fs.writeFileSync(resultFile, `PASS\n${log.join('\n')}\n`);
  } catch (error) {
    if (resultFile) fs.writeFileSync(resultFile, `FAIL\n${String(error)}\n`);
    throw error;
  }
}
