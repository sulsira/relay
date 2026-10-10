package com.example.sendtoterminal

import com.example.sendtoterminal.format.RelayPayloadFormatter
import com.example.sendtoterminal.settings.SendToTerminalSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class SendFilePathToTerminalAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val settings = service<SendToTerminalSettings>()
        TerminalSender.send(project) { agent ->
            RelayPayloadFormatter.formatFileReference(project, file, agent, settings)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled =
            e.project != null && e.getData(CommonDataKeys.VIRTUAL_FILE) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
