package com.example.sendtoterminal

import com.example.sendtoterminal.settings.SendToTerminalSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class DefaultAgentActionGroup : DefaultActionGroup() {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) {
            return EMPTY_ARRAY
        }

        val settings = service<SendToTerminalSettings>()
        settings.ensureInitialized()
        return settings.agents().map { agent ->
            SetDefaultAgentAction(agent.name)
        }.toTypedArray()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class SetDefaultAgentAction(
    private val agentName: String,
) : DumbAwareAction(
    agentName,
    "Use $agentName as the default Relay agent",
    null,
) {
    override fun actionPerformed(e: AnActionEvent) {
        service<SendToTerminalSettings>().setDefaultAgent(agentName)
    }

    override fun update(e: AnActionEvent) {
        val currentDefault = service<SendToTerminalSettings>().defaultAgent().name
        e.presentation.icon = if (agentName == currentDefault) AllIcons.Actions.Checked else null
        e.presentation.isEnabled = agentName != currentDefault
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
