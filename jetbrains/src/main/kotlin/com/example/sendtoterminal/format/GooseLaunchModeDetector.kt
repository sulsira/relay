package com.example.sendtoterminal.format

import com.example.sendtoterminal.settings.AgentProfile

object GooseLaunchModeDetector {
    private val INTERACTIVE_FLAGS = setOf("-s", "--interactive")
    private val NON_INTERACTIVE_SUBCOMMANDS = setOf(
        "serve",
        "configure",
        "info",
        "mcp",
        "project",
        "recipe",
        "schedule",
        "update",
        "bench",
        "completion",
        "extension",
        "extensions",
        "providers",
        "models",
    )

    fun detect(profile: AgentProfile): AgentLaunchMode {
        if (profile.launchMode != AgentLaunchMode.AUTO) {
            return profile.launchMode
        }
        if (profile.kind() != AgentKind.GOOSE) {
            return AgentLaunchMode.PLAIN
        }
        return detectFromCommand(profile.command)
    }

    fun detectFromCommand(command: String): AgentLaunchMode {
        val tokens = tokenize(command)
        if (tokens.isEmpty()) return AgentLaunchMode.INTERACTIVE

        if (looksLikeDesktopLaunch(tokens)) {
            return AgentLaunchMode.INTERACTIVE
        }

        val gooseIndex = tokens.indexOfFirst { token ->
            token.substringAfterLast('/').equals("goose", ignoreCase = true)
        }
        if (gooseIndex < 0) return AgentLaunchMode.PLAIN

        val args = tokens.drop(gooseIndex + 1).map { it.lowercase() }
        if (args.isEmpty()) return AgentLaunchMode.INTERACTIVE

        val subcommand = args[0]
        if (subcommand.startsWith('-') || subcommand in NON_INTERACTIVE_SUBCOMMANDS) {
            return AgentLaunchMode.PLAIN
        }

        return when (subcommand) {
            "session" -> AgentLaunchMode.INTERACTIVE
            "run" -> when {
                args.any { it in INTERACTIVE_FLAGS } -> AgentLaunchMode.INTERACTIVE
                args.any { it == "-i" || it == "--instruction" } -> AgentLaunchMode.PLAIN
                else -> AgentLaunchMode.PLAIN
            }
            else -> AgentLaunchMode.INTERACTIVE
        }
    }

    private fun looksLikeDesktopLaunch(tokens: List<String>): Boolean {
        val lower = tokens.map { it.lowercase() }
        if ("open" !in lower) return false
        return lower.any { token ->
            token == "goose" ||
                token == "block-goose" ||
                token.endsWith("/goose.app") ||
                token.contains("block-goose")
        }
    }

    private fun tokenize(command: String): List<String> =
        command.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
}
