import * as vscode from 'vscode';
import { AgentProfile, strategyForProfile } from './format';
import * as sender from './sender';
import * as settings from './settings';

function relativePath(uri: vscode.Uri): string {
  return vscode.workspace.asRelativePath(uri, false).replace(/\\/g, '/');
}

async function pickAgent(placeHolder: string): Promise<AgentProfile | undefined> {
  const current = settings.defaultAgent().name;
  const items = settings.agents().map((agent) => ({
    label: agent.name,
    description: agent.name === current ? 'default' : undefined,
    detail: agent.command,
    agent,
  }));
  const picked = await vscode.window.showQuickPick(items, { placeHolder, matchOnDetail: true });
  return picked?.agent;
}

function sendFilePath(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.uri.scheme !== 'file') {
    void vscode.window.showWarningMessage('Relay: open a file to send its path.');
    return Promise.resolve();
  }

  const path = relativePath(editor.document.uri);
  return sender.send((agent) => strategyForProfile(agent).file(path));
}

function sendSelection(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.uri.scheme !== 'file' || editor.selection.isEmpty) {
    void vscode.window.showWarningMessage('Relay: select some code to send.');
    return Promise.resolve();
  }

  const path = relativePath(editor.document.uri);
  const startLine = editor.selection.start.line + 1;
  const endLine = editor.selection.end.line + 1;
  return sender.send((agent) => strategyForProfile(agent).selection(path, startLine, endLine));
}

async function openAgentCommand(name?: unknown): Promise<void> {
  if (typeof name === 'string' && name) {
    sender.openAgent(name);
    return;
  }
  const agent = await pickAgent('Open which agent?');
  if (!agent) return;
  sender.openAgent(agent.name);
}

async function setDefaultAgentCommand(): Promise<void> {
  const agent = await pickAgent('Choose the default agent');
  if (!agent) return;
  await settings.setDefaultAgent(agent.name);
}

export function activate(context: vscode.ExtensionContext): void {
  const statusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 0);
  statusBar.command = 'relay.openAgent';
  statusBar.tooltip = 'Relay: open an agent';

  const refreshStatusBar = (): void => {
    statusBar.text = `$(terminal) Relay: ${sender.targetAgent().name}`;
    statusBar.show();
  };

  sender.register(context, refreshStatusBar);
  refreshStatusBar();

  context.subscriptions.push(
    statusBar,
    vscode.commands.registerCommand('relay.open', () => sender.openAndFocus()),
    vscode.commands.registerCommand('relay.openAgent', openAgentCommand),
    vscode.commands.registerCommand('relay.sendFilePath', sendFilePath),
    vscode.commands.registerCommand('relay.sendSelection', sendSelection),
    vscode.commands.registerCommand('relay.setDefaultAgent', setDefaultAgentCommand),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('relay')) refreshStatusBar();
    }),
  );
}

export function deactivate(): void {}
