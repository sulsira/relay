package com.example.sendtoterminal

import com.example.sendtoterminal.format.RelayPayloadFormatter
import com.example.sendtoterminal.settings.SendToTerminalSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class SendSelectionToTerminalAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: editor.virtualFile ?: return
        val selected = editor.selectionModel.selectedText
        if (selected.isNullOrEmpty()) return

        val settings = service<SendToTerminalSettings>()
        TerminalSender.send(project) { agent ->
            RelayPayloadFormatter.formatSelection(project, file, editor, selected, agent, settings)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled =
            e.project != null && editor?.selectionModel?.hasSelection() == true
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
