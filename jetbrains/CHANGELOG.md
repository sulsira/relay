<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Relay Changelog

## [Unreleased]

## [1.5.0] - 2026-10-10

### Added

- Stale `Relay:*` terminal tabs restored from a previous IDE session are closed on startup, so Relay always opens a fresh tab
- Per-agent **Open <agent>** actions, bindable under Settings > Keymap > Relay (no default shortcut)
- One Relay tab per agent; sessions are tracked separately and survive switching between them

### Changed

- File path and selection sends go to the active Relay tab (selected, else last selected) and are formatted for that tab's agent. The default agent is used only when no Relay tab is open

## [1.4.2] - 2026-06-10

### Changed

- Aider selections paste a single plain string (file, lines, fenced snippet) instead of a two-step `/read-only` flow
- Non-executing sends use bracketed paste so multiline snippets are not submitted line by line

## [1.4.1] - 2026-06-10

### Changed

- File paths and selections are pasted into the agent terminal without pressing Enter, so you can add more context before submitting

## [1.4.0] - 2026-06-10

### Added

- Goose launch-mode detection (interactive TUI vs plain CLI) from the agent command
- `@path` file references for interactive Goose sessions (`goose`, `goose session`, `goose run -s`)
- Per-agent **Launch** column in settings (`Auto`, `Interactive`, `Plain`)

### Changed

- Goose batch commands (`goose run`, `goose run -i …`) keep the plain `Read path` format

## [1.3.0] - 2026-06-10

### Added

- Two-step send for Aider selections: slash command first, then line focus and snippet
- Per-agent **Reference** mode in settings (`Auto`, `Add`, `Read-only`, `@ mention`, `Plain`)

## [1.2.0] - 2026-06-10

### Added

- Agent-aware file references based on the default agent:
  - **Cursor / Claude:** `@path` and native line-range syntax for selections
  - **Aider:** `/add path` for file paths, `/read-only path` plus fenced snippet for selections
  - **Goose / custom agents:** existing `Read path` format

## [1.1.2] - 2026-06-10

### Fixed

- Terminal still launched the legacy 1.0.x agent command instead of the selected default agent

## [1.1.1] - 2026-06-10

### Fixed

- Stack overflow when loading agent settings caused by recursive initialization

## [1.1.0] - 2026-06-10

### Added

- Agent library with multiple saved agents (name + command)
- Default agent selection in settings
- Quick default switch via **Relay → Default Agent** in the editor menu
- Clean file references (`Read path/to/file.kt`) and fenced code snippets for selections

### Changed

- Shortcuts always target the current default agent; changing default opens a fresh `Relay:<agent>` tab on next use

## [1.0.0] - 2026-06-10

### Changed

- First stable release as **Relay** with plugin ID `com.sulsira.relay`

## [0.0.4] - 2026-06-10

### Added

- Configurable agent name and command (Cursor, Claude Code, Aider, etc.)
- Relay settings page with agent configuration and shortcut editing
- Plugin description with instructions for switching agents

### Changed

- Renamed plugin to Relay; terminal tab title is now `Relay:<agent>`
- Agent-agnostic action names and context menu

## [0.0.3] - 2026-06-10

### Added

- Plugin description with feature overview and keyboard shortcuts in the Installed Plugins list

### Fixed

- Focus the existing Cursor Agent tab by terminal view identity instead of tab title

## [0.0.2] - 2026-06-10

### Added

- Send selections with file path and line range
- Start Cursor Agent automatically with the `agent` command

### Changed

- Renamed actions and plugin display name to reference Cursor Agent

## [0.0.1] - 2026-06-10

### Added

- Initial plugin with dedicated terminal tab support
- Send file path, send selection, and open terminal shortcuts
