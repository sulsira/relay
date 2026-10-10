# Relay for VS Code Changelog

## [1.5.1] - 2026-10-10

### Changed

- README links to the GitHub repository for source, issues, the JetBrains version and the changelog
- Installable files are published under `releases/` in the repository

## [1.5.0] - 2026-10-10

### Added

- Initial VS Code port of Relay: send file paths and selections to a CLI agent in a `Relay:<agent>` terminal
- Agent profiles in `relay.agents` with `referenceMode` (`auto`, `add`, `read-only`, `at-mention`, `plain`)
- Sends go to the active Relay terminal, else the last one used, else a new terminal for `relay.defaultAgent`
- `Relay: Open Agent...` command, bindable per agent with a keybinding argument
- Relay terminals restored from a previous window session are closed on startup
- Status bar item showing the agent that receives the next send
