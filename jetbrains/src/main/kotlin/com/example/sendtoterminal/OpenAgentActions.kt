package com.example.sendtoterminal

import com.example.sendtoterminal.settings.AgentProfile
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAwareAction

private class OpenAgentAction(private val agentName: String) : DumbAwareAction(
    "Open $agentName",
    "Open and focus the Relay tab for $agentName",
    null,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        TerminalSender.openAgent(project, agentName)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

/**
 * One keymap-bindable "Open <agent>" action per agent profile. No default
 * shortcut is assigned; users bind keys under Settings > Keymap > Relay.
 */
object OpenAgentActions {

    fun idFor(agentName: String): String = "${SendToTerminalActionIds.OPEN_AGENT_PREFIX}$agentName"

    fun shortcutsFor(agentName: String): Array<Shortcut> =
        KeymapManager.getInstance().activeKeymap.getShortcuts(idFor(agentName))

    /** Remove every shortcut bound to the agent's open action in the active keymap. */
    fun clearShortcuts(agentName: String) {
        KeymapManager.getInstance().activeKeymap.removeAllActionShortcuts(idFor(agentName))
    }

    /** Register actions for [agents] and unregister the ones that no longer have a profile. */
    fun sync(agents: List<AgentProfile>) {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction(SendToTerminalActionIds.GROUP) as? DefaultActionGroup ?: return
        val wanted = agents.map { idFor(it.name) }.toSet()
        val registered = actionManager.getActionIdList(SendToTerminalActionIds.OPEN_AGENT_PREFIX).toSet()

        registered.filter { it !in wanted }.forEach { id ->
            group.remove(actionManager.getAction(id))
            actionManager.unregisterAction(id)
        }
        agents.filter { idFor(it.name) !in registered }.forEach { agent ->
            val action = OpenAgentAction(agent.name)
            actionManager.registerAction(idFor(agent.name), action)
            group.add(action)
        }
    }
}
