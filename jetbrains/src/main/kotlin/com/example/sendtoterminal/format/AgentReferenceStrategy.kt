package com.example.sendtoterminal.format

object ReferenceFormatContext

interface AgentReferenceStrategy {
    fun formatFileReference(relativePath: String, context: ReferenceFormatContext): RelayPayload

    fun formatSelection(
        relativePath: String,
        startLine: Int,
        endLine: Int,
        language: String,
        snippet: String,
        context: ReferenceFormatContext,
    ): RelayPayload
}
