package com.example.sendtoterminal.format

import com.example.sendtoterminal.settings.AgentProfile

enum class AgentKind {
    CURSOR,
    CLAUDE,
    AIDER,
    GOOSE,
    GENERIC,
}

fun AgentProfile.kind(): AgentKind {
    val trimmedCommand = command.trim()
    val commandToken = trimmedCommand.substringBefore(' ').lowercase()

    return when {
        name.equals("cursor", ignoreCase = true) || commandToken == "agent" -> AgentKind.CURSOR
        name.equals("claude", ignoreCase = true) || commandToken == "claude" -> AgentKind.CLAUDE
        name.equals("aider", ignoreCase = true) || commandToken == "aider" -> AgentKind.AIDER
        name.equals("goose", ignoreCase = true) || commandToken == "goose" -> AgentKind.GOOSE
        else -> AgentKind.GENERIC
    }
}
