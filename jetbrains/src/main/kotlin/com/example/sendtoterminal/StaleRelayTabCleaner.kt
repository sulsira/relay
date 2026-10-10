package com.example.sendtoterminal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.terminal.frontend.toolwindow.TerminalTabsManagerListener
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager

private const val RELAY_TAB_PREFIX = "Relay:"

/**
 * Closes `Relay:*` terminal tabs restored from a previous IDE session.
 * Their shell and agent process are gone, so Relay opens a fresh tab on first use instead.
 */
class StaleRelayTabCleaner : ProjectActivity {

    override suspend fun execute(project: Project) {
        val manager = TerminalToolWindowTabsManager.getInstance(project)
        val state = project.service<TerminalSenderState>()

        manager.addListener(state, object : TerminalTabsManagerListener {
            override fun tabAdded(tab: TerminalToolWindowTab) = closeIfStale(project, tab)
        })
        manager.tabs.forEach { tab -> closeIfStale(project, tab) }
    }

    private fun closeIfStale(project: Project, tab: TerminalToolWindowTab) {
        val state = project.service<TerminalSenderState>()
        if (state.creatingTab) return
        if (state.sessions.containsKey(tab.view)) return

        val title = tab.content.displayName.orEmpty()
        if (!title.startsWith(RELAY_TAB_PREFIX)) return

        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            TerminalToolWindowTabsManager.getInstance(project).closeTab(tab)
        }
    }
}
