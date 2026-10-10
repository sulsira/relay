export type ReferenceMode = 'auto' | 'add' | 'read-only' | 'at-mention' | 'plain';

export type AgentKind = 'cursor' | 'claude' | 'aider' | 'goose' | 'generic';

export interface AgentProfile {
  name: string;
  command: string;
  referenceMode: ReferenceMode;
}

export interface ReferenceStrategy {
  file(path: string): string;
  selection(path: string, startLine: number, endLine: number): string;
}

export const REFERENCE_MODES: readonly ReferenceMode[] = ['auto', 'add', 'read-only', 'at-mention', 'plain'];

export function parseReferenceMode(value: unknown): ReferenceMode {
  const match = REFERENCE_MODES.find((mode) => mode === value);
  return match ?? 'auto';
}

export function agentKind(profile: Pick<AgentProfile, 'name' | 'command'>): AgentKind {
  const name = profile.name.toLowerCase();
  const commandToken = profile.command.trim().split(/\s+/)[0].toLowerCase();

  if (name === 'cursor' || commandToken === 'agent') return 'cursor';
  if (name === 'claude' || commandToken === 'claude') return 'claude';
  if (name === 'aider' || commandToken === 'aider') return 'aider';
  if (name === 'goose' || commandToken === 'goose') return 'goose';
  return 'generic';
}

const atMention: ReferenceStrategy = {
  file: (path) => `@${path}`,
  selection: (path, start, end) => `@${path}:${start}-${end}`,
};

const claude: ReferenceStrategy = {
  file: (path) => `@${path}`,
  selection: (path, start, end) => `@${path}#L${start}-L${end}`,
};

function slashCommand(command: '/add' | '/read-only'): ReferenceStrategy {
  return {
    file: (path) => `${command} @${path}`,
    selection: atMention.selection,
  };
}

export function strategyForKind(kind: AgentKind): ReferenceStrategy {
  if (kind === 'claude') return claude;
  return atMention;
}

export function strategyForProfile(profile: AgentProfile): ReferenceStrategy {
  if (profile.referenceMode === 'add') return slashCommand('/add');
  if (profile.referenceMode === 'read-only') return slashCommand('/read-only');
  if (profile.referenceMode === 'at-mention') return atMention;
  if (profile.referenceMode === 'plain') return atMention;
  return strategyForKind(agentKind(profile));
}
