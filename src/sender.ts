import * as vscode from 'vscode';
import { AgentProfile } from './format';
import * as settings from './settings';

const TAB_PREFIX = 'Relay:';
const SHELL_READY_TIMEOUT_MS = 3000;

interface Session {
  agent: AgentProfile;
  ready: Promise<void>;
}

const sessions = new Map<vscode.Terminal, Session>();
let lastActive: vscode.Terminal | undefined;
let creatingTerminal = false;

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

function isAlive(terminal: vscode.Terminal): boolean {
  return terminal.exitStatus === undefined && vscode.window.terminals.includes(terminal);
}

function activeRelayTerminal(): vscode.Terminal | undefined {
  const active = vscode.window.activeTerminal;
  if (active && sessions.has(active)) return active;
  if (lastActive && sessions.has(lastActive) && isAlive(lastActive)) return lastActive;
  return undefined;
}

/** Agent that the next send targets: the active Relay terminal's agent, else the default. */
export function targetAgent(): AgentProfile {
  const terminal = activeRelayTerminal();
  if (!terminal) return settings.defaultAgent();
  const session = sessions.get(terminal);
  return session ? refreshed(session.agent) : settings.defaultAgent();
}

/** Re-read the profile so edited reference modes apply to terminals that are already running. */
function refreshed(agent: AgentProfile): AgentProfile {
  return settings.agents().find((profile) => profile.name === agent.name) ?? agent;
}

async function waitForShell(terminal: vscode.Terminal): Promise<void> {
  if (terminal.shellIntegration) return;

  const integrated = new Promise<void>((resolve) => {
    const listener = vscode.window.onDidChangeTerminalShellIntegration((event) => {
      if (event.terminal !== terminal) return;
      listener.dispose();
      resolve();
    });
    setTimeout(() => listener.dispose(), SHELL_READY_TIMEOUT_MS);
  });
  await Promise.race([integrated, sleep(SHELL_READY_TIMEOUT_MS)]);
}

async function startAgent(terminal: vscode.Terminal, agent: AgentProfile): Promise<void> {
  await waitForShell(terminal);
  if (!agent.command.trim()) return;
  terminal.sendText(agent.command, true);
  await sleep(settings.startupDelayMs());
}

function createRelayTerminal(agent: AgentProfile): vscode.Terminal {
  creatingTerminal = true;
  const terminal = (() => {
    try {
      return vscode.window.createTerminal({ name: `${TAB_PREFIX}${agent.name}` });
    } finally {
      creatingTerminal = false;
    }
  })();

  sessions.set(terminal, { agent, ready: startAgent(terminal, agent) });
  lastActive = terminal;
  return terminal;
}

function findAgentTerminal(agentName: string): vscode.Terminal | undefined {
  for (const [terminal, session] of sessions) {
    if (session.agent.name === agentName && isAlive(terminal)) return terminal;
  }
  return undefined;
}

function resolveActiveOrCreate(): vscode.Terminal {
  return activeRelayTerminal() ?? createRelayTerminal(settings.defaultAgent());
}

function focus(terminal: vscode.Terminal): void {
  lastActive = terminal;
  terminal.show(false);
}

async function runInSession(terminal: vscode.Terminal, block: (t: vscode.Terminal) => void): Promise<void> {
  const session = sessions.get(terminal);
  if (!session) return;
  await session.ready;
  block(terminal);
}

/** Open and focus the active Relay terminal, creating one for the default agent if none exists. */
export function openAndFocus(): void {
  focus(resolveActiveOrCreate());
}

/** Open and focus the terminal of [agentName], creating it on first use. */
export function openAgent(agentName: string): void {
  const agent = settings.agents().find((profile) => profile.name === agentName);
  if (!agent) {
    void vscode.window.showWarningMessage(`Relay: no agent named "${agentName}".`);
    return;
  }
  focus(findAgentTerminal(agent.name) ?? createRelayTerminal(agent));
}

/** Paste the reference formatted for the target terminal's agent. It is not submitted. */
export async function send(format: (agent: AgentProfile) => string): Promise<void> {
  const terminal = resolveActiveOrCreate();
  focus(terminal);
  const session = sessions.get(terminal);
  if (!session) return;
  const text = format(refreshed(session.agent));
  await runInSession(terminal, (t) => t.sendText(text, false));
}

/** Track terminal lifecycle and close Relay terminals restored from a previous window session. */
export function register(context: vscode.ExtensionContext, onChange: () => void): void {
  const closeIfStale = (terminal: vscode.Terminal): void => {
    if (creatingTerminal || sessions.has(terminal)) return;
    if (!terminal.name.startsWith(TAB_PREFIX)) return;
    terminal.dispose();
  };

  vscode.window.terminals.forEach(closeIfStale);

  context.subscriptions.push(
    vscode.window.onDidOpenTerminal(closeIfStale),
    vscode.window.onDidCloseTerminal((terminal) => {
      sessions.delete(terminal);
      if (lastActive === terminal) lastActive = undefined;
      onChange();
    }),
    vscode.window.onDidChangeActiveTerminal((terminal) => {
      if (terminal && sessions.has(terminal)) lastActive = terminal;
      onChange();
    }),
  );
}
