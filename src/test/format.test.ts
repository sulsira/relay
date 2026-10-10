import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { AgentProfile, agentKind, parseReferenceMode, strategyForKind, strategyForProfile } from '../format';

const profile = (name: string, command: string, referenceMode: AgentProfile['referenceMode'] = 'auto'): AgentProfile => ({
  name,
  command,
  referenceMode,
});

describe('agentKind', () => {
  it('detects agent kinds from profile name and command', () => {
    assert.equal(agentKind(profile('cursor', 'agent')), 'cursor');
    assert.equal(agentKind(profile('my-cursor', 'agent --resume')), 'cursor');
    assert.equal(agentKind(profile('claude', 'claude')), 'claude');
    assert.equal(agentKind(profile('aider', 'aider --model sonnet')), 'aider');
    assert.equal(agentKind(profile('goose', 'goose')), 'goose');
    assert.equal(agentKind(profile('codex', 'codex')), 'generic');
  });

  it('matches the name case-insensitively', () => {
    assert.equal(agentKind(profile('Claude', 'run')), 'claude');
  });
});

describe('reference strategies', () => {
  it('cursor uses at-mentions', () => {
    const strategy = strategyForKind('cursor');
    assert.equal(strategy.file('src/Foo.kt'), '@src/Foo.kt');
    assert.equal(strategy.selection('src/Foo.kt', 10, 25), '@src/Foo.kt:10-25');
  });

  it('claude uses at-mentions with line ranges', () => {
    const strategy = strategyForKind('claude');
    assert.equal(strategy.file('src/Foo.kt'), '@src/Foo.kt');
    assert.equal(strategy.selection('src/Foo.kt', 10, 25), '@src/Foo.kt#L10-L25');
  });

  it('aider and goose use at-mentions under auto', () => {
    assert.equal(strategyForProfile(profile('aider', 'aider')).file('a.kt'), '@a.kt');
    assert.equal(strategyForProfile(profile('goose', 'goose session')).file('a.kt'), '@a.kt');
  });

  it('generic uses at-mentions and keeps single-line ranges', () => {
    const strategy = strategyForKind('generic');
    assert.equal(strategy.file('src/Foo.kt'), '@src/Foo.kt');
    assert.equal(strategy.selection('src/Foo.kt', 10, 10), '@src/Foo.kt:10-10');
  });

  it('add override prefixes /add', () => {
    const strategy = strategyForProfile(profile('aider', 'aider', 'add'));
    assert.equal(strategy.file('src/Foo.kt'), '/add @src/Foo.kt');
    assert.equal(strategy.selection('src/Foo.kt', 1, 2), '@src/Foo.kt:1-2');
  });

  it('read-only override prefixes /read-only', () => {
    const strategy = strategyForProfile(profile('aider', 'aider', 'read-only'));
    assert.equal(strategy.file('src/Foo.kt'), '/read-only @src/Foo.kt');
  });

  it('at-mention and plain overrides ignore the agent kind', () => {
    assert.equal(strategyForProfile(profile('claude', 'claude', 'at-mention')).selection('a.kt', 1, 2), '@a.kt:1-2');
    assert.equal(strategyForProfile(profile('claude', 'claude', 'plain')).selection('a.kt', 1, 2), '@a.kt:1-2');
  });
});

describe('parseReferenceMode', () => {
  it('accepts known modes and falls back to auto', () => {
    assert.equal(parseReferenceMode('read-only'), 'read-only');
    assert.equal(parseReferenceMode('bogus'), 'auto');
    assert.equal(parseReferenceMode(undefined), 'auto');
  });
});
