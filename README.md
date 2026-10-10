# Relay

Relay sends editor context (file paths and code selections) to a CLI coding agent running in a dedicated terminal tab. It ships once per editor platform. Each platform lives in its own folder, side by side.

![Relay demo](jetbrains/media/relay-demo.gif)

_Illustrative animation, not a screen recording._

## Platforms

| Platform | Folder | Language | Build | Docs |
|---|---|---|---|---|
| JetBrains IDEs (IntelliJ IDEA, PyCharm, WebStorm, ...) | [`jetbrains/`](jetbrains/) | Kotlin | Gradle | [`jetbrains/README.md`](jetbrains/README.md) |
| VS Code | [`vscode/`](vscode/) | TypeScript | npm | [`vscode/README.md`](vscode/README.md) |

- The platforms are independent. Neither builds the other.
- Each folder has its own README, changelog, version, tests and build.
- Build, test and install steps are in each platform's README. This file does not repeat them.

## What Relay does

1. You press a shortcut in the editor.
2. Relay picks the target terminal: the active Relay terminal, else the last Relay terminal you used, else a new one for the default agent.
3. Relay starts that agent's command in the terminal, once.
4. Relay pastes a reference to your file or selection into the agent prompt, formatted for that agent. It does not press Enter.

| Input | Transform | Output |
|---|---|---|
| Active file | Project-relative path, formatted for the agent | `@src/Foo.kt` |
| Selected lines | Path plus line range, formatted for the agent | `@src/Foo.kt#L10-L20` |
| Agent profile | Command run in the terminal on first use | Running agent session |

A selection sends a reference, not the code. The agent reads the lines from the saved file.

## Shared behavior

Both platforms use the same terminal names, default keys and reference formats.

| Action | Default key |
|---|---|
| Open or focus Relay | `Ctrl+Alt+T` |
| Send file path | `Ctrl+Alt+P` |
| Send selection | `Ctrl+Alt+Enter` |

Reference formats for file `src/Foo.kt` and lines 10 to 20:

| Agent or mode | File reference | Selection reference |
|---|---|---|
| Cursor (`auto`) | `@src/Foo.kt` | `@src/Foo.kt:10-20` |
| Claude Code (`auto`) | `@src/Foo.kt` | `@src/Foo.kt#L10-L20` |
| Aider, Goose, other (`auto`) | `@src/Foo.kt` | `@src/Foo.kt:10-20` |
| `add` | `/add @src/Foo.kt` | `@src/Foo.kt:10-20` |
| `read-only` | `/read-only @src/Foo.kt` | `@src/Foo.kt:10-20` |
| `at-mention`, `plain` | `@src/Foo.kt` | `@src/Foo.kt:10-20` |

- Agent kind comes from the profile name or the first word of its command.
- The JetBrains plugin also has a **Launch** column (Goose interactive or plain). VS Code does not.

## Adding a new platform

Follow the same separation.

1. Create a folder named after the platform, for example `zed/`, next to `jetbrains/` and `vscode/`.
2. Keep everything for that platform inside it: source, build files, tests, `README.md`, `CHANGELOG.md`.
3. Use the shared key table and reference formats above.
4. Add a row to the **Platforms** table in this file.
5. Tag releases with the platform prefix, for example `zed-v1.0.0`.
6. Add the build output of the new platform to the root `.gitignore`.

## Releases

| Platform | Tag | Version lives in |
|---|---|---|
| JetBrains | `jetbrains-v<version>` | `jetbrains/gradle.properties` |
| VS Code | `vscode-v<version>` | `vscode/package.json` |

Consequence: a fix to a shared reference format must be made in each platform folder, so update the shared table above in the same commit.
