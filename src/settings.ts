import * as vscode from 'vscode';
import { AgentProfile, parseReferenceMode } from './format';

const SECTION = 'relay';

const PRESET_AGENTS: AgentProfile[] = [
  { name: 'cursor', command: 'agent', referenceMode: 'auto' },
  { name: 'claude', command: 'claude', referenceMode: 'auto' },
  { name: 'aider', command: 'aider --model sonnet', referenceMode: 'auto' },
];

const DEFAULT_STARTUP_DELAY_MS = 500;

function toProfile(raw: unknown): AgentProfile | undefined {
  if (typeof raw !== 'object' || raw === null) return undefined;
  const entry = raw as Record<string, unknown>;
  const name = typeof entry.name === 'string' ? entry.name.trim() : '';
  const command = typeof entry.command === 'string' ? entry.command.trim() : '';
  if (!name || !command) return undefined;
  return { name, command, referenceMode: parseReferenceMode(entry.referenceMode) };
}

function uniqueByName(profiles: AgentProfile[]): AgentProfile[] {
  const seen = new Set<string>();
  return profiles.filter((profile) => {
    if (seen.has(profile.name)) return false;
    seen.add(profile.name);
    return true;
  });
}

export function agents(): AgentProfile[] {
  const configured = vscode.workspace.getConfiguration(SECTION).get<unknown[]>('agents') ?? [];
  const profiles = uniqueByName(configured.map(toProfile).filter((p): p is AgentProfile => p !== undefined));
  return profiles.length > 0 ? profiles : PRESET_AGENTS;
}

export function defaultAgent(): AgentProfile {
  const all = agents();
  const name = vscode.workspace.getConfiguration(SECTION).get<string>('defaultAgent') ?? '';
  return all.find((profile) => profile.name === name) ?? all[0];
}

export function setDefaultAgent(name: string): Thenable<void> {
  return vscode.workspace
    .getConfiguration(SECTION)
    .update('defaultAgent', name, vscode.ConfigurationTarget.Global);
}

export function startupDelayMs(): number {
  const value = vscode.workspace.getConfiguration(SECTION).get<number>('agentStartupDelayMs');
  if (typeof value !== 'number' || value < 0) return DEFAULT_STARTUP_DELAY_MS;
  return value;
}
