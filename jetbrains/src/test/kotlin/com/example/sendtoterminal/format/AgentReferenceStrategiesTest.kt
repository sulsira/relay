package com.example.sendtoterminal.format

import com.example.sendtoterminal.settings.AgentProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentReferenceStrategiesTest {

    private val context = ReferenceFormatContext

    @Test
    fun `detects agent kinds from profile name and command`() {
        assertEquals(AgentKind.CURSOR, AgentProfile("cursor", "agent").kind())
        assertEquals(AgentKind.CURSOR, AgentProfile("my-cursor", "agent --resume").kind())
        assertEquals(AgentKind.CLAUDE, AgentProfile("claude", "claude").kind())
        assertEquals(AgentKind.AIDER, AgentProfile("aider", "aider --model sonnet").kind())
        assertEquals(AgentKind.GOOSE, AgentProfile("goose", "goose").kind())
        assertEquals(AgentKind.GENERIC, AgentProfile("codex", "codex").kind())
    }

    @Test
    fun `cursor uses at-mentions`() {
        val strategy = AgentReferenceStrategies.forKind(AgentKind.CURSOR)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
        assertEquals(
            "@src/Foo.kt:10-25",
            strategy.formatSelection("src/Foo.kt", 10, 25, "kotlin", "code", context).steps.single().text,
        )
    }

    @Test
    fun `claude uses at-mentions with line ranges`() {
        val strategy = AgentReferenceStrategies.forKind(AgentKind.CLAUDE)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
        assertEquals(
            "@src/Foo.kt#L10-L25",
            strategy.formatSelection("src/Foo.kt", 10, 25, "kotlin", "code", context).steps.single().text,
        )
    }

    @Test
    fun `reference payloads paste without executing`() {
        val strategy = AgentReferenceStrategies.forKind(AgentKind.AIDER)

        assertEquals(
            false,
            strategy.formatFileReference("src/Foo.kt", context).steps.single().execute,
        )

        val selection = strategy.formatSelection("src/Foo.kt", 10, 25, "kotlin", "code", context)
        assertTrue(selection.steps.all { !it.execute })
    }

    @Test
    fun `aider uses slash command for file path and plain snippet for selection`() {
        val strategy = AgentReferenceStrategies.forKind(AgentKind.AIDER)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )

        val selection = strategy.formatSelection("src/Foo.kt", 10, 25, "kotlin", "code", context)
        assertEquals(1, selection.steps.size)
        assertEquals(
            "@src/Foo.kt:10-25",
            selection.steps.single().text,
        )
    }

    @Test
    fun `aider read-only override uses read-only for file path`() {
        val profile = AgentProfile("aider", "aider", AgentReferenceMode.READ_ONLY)
        val strategy = AgentReferenceStrategies.forProfile(profile)

        assertEquals(
            "/read-only @src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
    }

    @Test
    fun `plain override uses read instructions`() {
        val profile = AgentProfile("cursor", "agent", AgentReferenceMode.PLAIN)
        val strategy = AgentReferenceStrategies.forProfile(profile)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
    }

    @Test
    fun `generic uses plain read instructions`() {
        val strategy = AgentReferenceStrategies.forKind(AgentKind.GENERIC)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
        assertEquals(
            "@src/Foo.kt:10-10",
            strategy.formatSelection("src/Foo.kt", 10, 10, "kotlin", "code", context).steps.single().text,
        )
    }

    @Test
    fun `goose interactive session uses at-mentions for file path`() {
        val profile = AgentProfile("goose", "goose session")
        val strategy = AgentReferenceStrategies.forProfile(profile)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
    }

    @Test
    fun `goose run batch mode uses plain read instructions`() {
        val profile = AgentProfile("goose", "goose run -i task.md")
        val strategy = AgentReferenceStrategies.forProfile(profile)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
    }

    @Test
    fun `goose launch override forces plain mode`() {
        val profile = AgentProfile(
            name = "goose",
            command = "goose session",
            launchMode = AgentLaunchMode.PLAIN,
        )
        val strategy = AgentReferenceStrategies.forProfile(profile)

        assertEquals(
            "@src/Foo.kt",
            strategy.formatFileReference("src/Foo.kt", context).steps.single().text,
        )
    }
}
