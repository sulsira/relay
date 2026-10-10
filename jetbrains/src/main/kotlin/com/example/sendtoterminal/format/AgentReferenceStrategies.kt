package com.example.sendtoterminal.format

import com.example.sendtoterminal.settings.AgentProfile

object AgentReferenceStrategies {

    fun forProfile(profile: AgentProfile): AgentReferenceStrategy =
        when (profile.referenceMode) {
            AgentReferenceMode.AUTO -> forKind(profile.kind(), profile)
            AgentReferenceMode.ADD -> AiderReferenceStrategy(fileSlashCommand = "/add")
            AgentReferenceMode.READ_ONLY -> AiderReferenceStrategy(fileSlashCommand = "/read-only")
            AgentReferenceMode.AT_MENTION -> CursorReferenceStrategy
            AgentReferenceMode.PLAIN -> GenericReferenceStrategy
        }

    fun forKind(kind: AgentKind, profile: AgentProfile? = null): AgentReferenceStrategy = when (kind) {
        AgentKind.CURSOR -> CursorReferenceStrategy
        AgentKind.CLAUDE -> ClaudeReferenceStrategy
        AgentKind.AIDER -> CursorReferenceStrategy
        AgentKind.GOOSE -> gooseStrategyFor(profile ?: AgentProfile("goose", "goose"))
        AgentKind.GENERIC -> CursorReferenceStrategy
    }

    private fun gooseStrategyFor(profile: AgentProfile): AgentReferenceStrategy =
        when (GooseLaunchModeDetector.detect(profile)) {
            AgentLaunchMode.INTERACTIVE -> GooseInteractiveReferenceStrategy
            AgentLaunchMode.PLAIN, AgentLaunchMode.AUTO -> GenericReferenceStrategy
        }
}

private object CursorReferenceStrategy : AgentReferenceStrategy {
    override fun formatFileReference(relativePath: String, context: ReferenceFormatContext): RelayPayload =
        RelayPayload.message("@$relativePath")

    override fun formatSelection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
        context: ReferenceFormatContext,
    ): RelayPayload = RelayPayload.message("@$relativePath:$startLine-$endLine")
}

private object ClaudeReferenceStrategy : AgentReferenceStrategy {
    override fun formatFileReference(relativePath: String, context: ReferenceFormatContext): RelayPayload =
        RelayPayload.message("@$relativePath")

    override fun formatSelection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
        context: ReferenceFormatContext,
    ): RelayPayload = RelayPayload.message("@$relativePath#L$startLine-L$endLine")
}

private class AiderReferenceStrategy(
    private val fileSlashCommand: String,
) : AgentReferenceStrategy {
    override fun formatFileReference(relativePath: String, context: ReferenceFormatContext): RelayPayload =
        RelayPayload.message("$fileSlashCommand @$relativePath")

    override fun formatSelection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
        context: ReferenceFormatContext,
    ): RelayPayload = RelayPayload.message(
        PlainReferenceFormat.selection(relativePath, startLine, endLine, language, snippet),
    )
}

private object GooseInteractiveReferenceStrategy : AgentReferenceStrategy {
    override fun formatFileReference(relativePath: String, context: ReferenceFormatContext): RelayPayload =
        RelayPayload.message("@$relativePath")

    override fun formatSelection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
        context: ReferenceFormatContext,
    ): RelayPayload = RelayPayload.message(
        PlainReferenceFormat.selection(relativePath, startLine, endLine, language, snippet),
    )
}

private object GenericReferenceStrategy : AgentReferenceStrategy {
    override fun formatFileReference(relativePath: String, context: ReferenceFormatContext): RelayPayload =
        RelayPayload.message(PlainReferenceFormat.file(relativePath))

    override fun formatSelection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
        context: ReferenceFormatContext,
    ): RelayPayload = RelayPayload.message(
        PlainReferenceFormat.selection(relativePath, startLine, endLine, language, snippet),
    )
}

internal object PlainReferenceFormat {
    fun file(relativePath: String): String = "@$relativePath"

    fun selection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
    ): String = "@$relativePath:$startLine-$endLine"
}

internal fun formatLineRange(startLine: Int, endLine: Int): String =
    if (startLine == endLine) "line $startLine" else "lines $startLine-$endLine"
