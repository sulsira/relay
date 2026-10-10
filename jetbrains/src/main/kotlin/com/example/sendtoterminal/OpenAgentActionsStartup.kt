package com.example.sendtoterminal

import com.example.sendtoterminal.settings.SendToTerminalSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class OpenAgentActionsStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        OpenAgentActions.sync(service<SendToTerminalSettings>().agents())
    }
}
