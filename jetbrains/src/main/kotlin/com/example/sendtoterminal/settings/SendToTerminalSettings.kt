package com.example.sendtoterminal.settings

import com.example.sendtoterminal.format.AgentLaunchMode
import com.example.sendtoterminal.format.AgentReferenceMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@State(name = "SendToTerminalSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)])
class SendToTerminalSettings :
    SimplePersistentStateComponent<SendToTerminalSettings.State>(State()) {

    class State : BaseState() {
        var agents by list<AgentProfileState>()
        var defaultAgentName by string(DEFAULT_AGENT_NAME)
        var agentStartupDelayMs by property(DEFAULT_AGENT_STARTUP_DELAY_MS)

        // Legacy fields from 1.0.x — used once to migrate into [agents].
        var agentName by string()
        var agentCommand by string()
    }

    fun ensureInitialized() {
        migrateLegacyAgentIfNeeded()
        clearStaleLegacyFields()
        resolveDefaultAgentName()
    }

    fun agents(): List<AgentProfile> {
        ensureInitialized()
        return readAgentsFromState()
    }

    fun defaultAgent(): AgentProfile {
        ensureInitialized()
        val configured = readAgentsFromState()
        val name = state.defaultAgentName.orEmpty()
        return configured.firstOrNull { it.name == name }
            ?: configured.firstOrNull()
            ?: AgentProfile(DEFAULT_AGENT_NAME, DEFAULT_AGENT_COMMAND)
    }

    fun setDefaultAgent(name: String) {
        ensureInitialized()
        if (readAgentNamesFromState().contains(name)) {
            state.defaultAgentName = name
            ApplicationManager.getApplication().saveSettings()
        }
    }

    fun tabTitle(): String = "Relay:${defaultAgent().name}"

    fun configFingerprint(): String {
        val agent = defaultAgent()
        return "${agent.name}|${agent.command}"
    }

    fun replaceAgents(agents: List<AgentProfile>, defaultAgentName: String) {
        state.agents.clear()
        agents.forEach { profile ->
            state.agents.add(AgentProfileState().apply {
                name = profile.name
                command = profile.command
                referenceMode = profile.referenceMode.name
                launchMode = profile.launchMode.name
            })
        }
        state.defaultAgentName = defaultAgentName
        clearStaleLegacyFields()
    }

    private fun migrateLegacyAgentIfNeeded() {
        if (state.agents.isNotEmpty()) return

        val legacyName = state.agentName?.trim().orEmpty().ifBlank { DEFAULT_AGENT_NAME }
        val legacyCommand = state.agentCommand?.trim().orEmpty().ifBlank { DEFAULT_AGENT_COMMAND }
        state.agents.add(AgentProfileState().apply {
            name = legacyName
            command = legacyCommand
        })
        if (state.defaultAgentName.isNullOrBlank()) {
            state.defaultAgentName = legacyName
        }
        clearStaleLegacyFields()
    }

    /** Drop 1.0.x fields once [agents] is the source of truth. */
    private fun clearStaleLegacyFields() {
        if (state.agents.isEmpty()) return
        state.agentName = null
        state.agentCommand = null
    }

    private fun resolveDefaultAgentName() {
        val names = readAgentNamesFromState()
        if (names.isEmpty()) return
        if (state.defaultAgentName.orEmpty() !in names) {
            state.defaultAgentName = names.first()
        }
    }

    private fun readAgentsFromState(): List<AgentProfile> =
        state.agents.mapNotNull { entry ->
            val name = entry.name?.trim().orEmpty()
            val command = entry.command?.trim().orEmpty()
            if (name.isEmpty() || command.isEmpty()) {
                null
            } else {
                AgentProfile(
                    name = name,
                    command = command,
                    referenceMode = AgentReferenceMode.fromStored(entry.referenceMode),
                    launchMode = AgentLaunchMode.fromStored(entry.launchMode),
                )
            }
        }

    private fun readAgentNamesFromState(): List<String> =
        state.agents.mapNotNull { entry ->
            entry.name?.trim()?.takeIf { it.isNotEmpty() }
        }

    companion object {
        const val DEFAULT_AGENT_NAME = "cursor"
        const val DEFAULT_AGENT_COMMAND = "agent"
        const val DEFAULT_AGENT_STARTUP_DELAY_MS = 500

        val PRESET_AGENTS = listOf(
            AgentProfile("cursor", "agent"),
            AgentProfile("claude", "claude"),
            AgentProfile("aider", "aider --model sonnet"),
        )
    }
}
