package com.example.sendtoterminal

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.example.sendtoterminal.format.RelayPayload
import com.example.sendtoterminal.settings.AgentProfile
import com.example.sendtoterminal.settings.SendToTerminalSettings
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// NOTE ON EXPERIMENTAL API ---------------------------------------------------
// The Reworked Terminal public API (2025.3+) is experimental, so exact class
// locations / method names can shift between releases. Source of truth:
//   TerminalView:
//     plugins/terminal/frontend/src/com/intellij/terminal/frontend/view/TerminalView.kt
//   TerminalToolWindowTabsManager:
//     plugins/terminal/frontend/src/com/intellij/terminal/frontend/toolwindow/
//       TerminalToolWindowTabsManager.kt
// Cmd/Ctrl+click each symbol in the IDE to confirm against your version.
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.intellij.terminal.frontend.view.TerminalView

/** One Relay terminal tab: the agent it runs and the job that starts that agent. */
class RelaySession(val agent: AgentProfile, val ready: Deferred<Unit>)

/**
 * Project-level service that hands us a coroutine scope (needed to await
 * the shell becoming ready) and tracks every Relay tab by view identity,
 * plus the Relay tab the user selected most recently.
 */
@Service(Service.Level.PROJECT)
class TerminalSenderState(val scope: CoroutineScope) : Disposable {
    val sessions = ConcurrentHashMap<TerminalView, RelaySession>()
    @Volatile var lastActive: TerminalView? = null
    @Volatile var creatingTab: Boolean = false
    @Volatile var selectionTracked: Boolean = false

    override fun dispose() = Unit
}

object TerminalSender {

    private fun settings(): SendToTerminalSettings =
        ApplicationManager.getApplication().service()

    // --- public actions -----------------------------------------------------

    /**
     * Send to the active Relay tab, formatting the payload for that tab's agent.
     * Opens a tab for the default agent when no Relay tab exists.
     */
    fun send(project: Project, format: (AgentProfile) -> RelayPayload) {
        val view = resolveActiveOrCreate(project)
        focusTerminal(project, view)
        val session = project.service<TerminalSenderState>().sessions.getValue(view)
        val payload = format(session.agent)
        runInSession(project, view) { terminal -> sendPayload(terminal, payload) }
    }

    /** Open and focus the active Relay tab, creating one for the default agent if none exists. */
    fun openAndFocus(project: Project) {
        val view = resolveActiveOrCreate(project)
        focusTerminal(project, view)
        runInSession(project, view) { }
    }

    /** Open and focus the Relay tab of [agentName], creating it on first use. */
    fun openAgent(project: Project, agentName: String) {
        val agent = settings().agents().firstOrNull { it.name == agentName } ?: return
        val view = findAgentView(project, agent.name) ?: createRelayTab(project, agent)
        focusTerminal(project, view)
        runInSession(project, view) { }
    }

    // --- internals -----------------------------------------------------------

    private fun resolveActiveOrCreate(project: Project): TerminalView {
        pruneClosedSessions(project)
        val active = activeRelayView(project)
        if (active != null) return active
        return createRelayTab(project, settings().defaultAgent())
    }

    /** The selected Relay tab, else the last Relay tab the user selected. */
    private fun activeRelayView(project: Project): TerminalView? {
        val state = project.service<TerminalSenderState>()
        val tabs = TerminalToolWindowTabsManager.getInstance(project).tabs
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
        val selectedContent = toolWindow?.contentManager?.selectedContent
        val selectedView = tabs.firstOrNull { it.content === selectedContent }?.view
        if (selectedView != null && state.sessions.containsKey(selectedView)) return selectedView
        return state.lastActive?.takeIf { state.sessions.containsKey(it) }
    }

    private fun findAgentView(project: Project, agentName: String): TerminalView? {
        pruneClosedSessions(project)
        val state = project.service<TerminalSenderState>()
        return state.sessions.entries.firstOrNull { it.value.agent.name == agentName }?.key
    }

    private fun pruneClosedSessions(project: Project) {
        val state = project.service<TerminalSenderState>()
        val openViews = TerminalToolWindowTabsManager.getInstance(project).tabs.map { it.view }
        state.sessions.keys.removeAll { view -> openViews.none { it === view } }
    }

    private fun createRelayTab(project: Project, agent: AgentProfile): TerminalView {
        val state = project.service<TerminalSenderState>()
        val manager = TerminalToolWindowTabsManager.getInstance(project)

        // The tab-added listener fires inside createTab(); the flag keeps the
        // stale-tab cleaner from closing the tab we are creating.
        state.creatingTab = true
        val tab = try {
            manager.createTabBuilder().tabName("Relay:${agent.name}").createTab()
        } finally {
            state.creatingTab = false
        }

        val view = tab.view
        val ready = state.scope.async { startAgent(view, agent) }
        state.sessions[view] = RelaySession(agent, ready)
        state.lastActive = view
        trackSelection(project, tab)
        return view
    }

    /** Remember which Relay tab the user selects, so sends follow the tab they last used. */
    private fun trackSelection(project: Project, tab: TerminalToolWindowTab) {
        val state = project.service<TerminalSenderState>()
        if (state.selectionTracked) return
        val contentManager = tab.content.manager ?: return
        state.selectionTracked = true

        val listener = object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                if (event.operation != ContentManagerEvent.ContentOperation.add) return
                val tabs = TerminalToolWindowTabsManager.getInstance(project).tabs
                val view = tabs.firstOrNull { it.content === event.content }?.view ?: return
                if (state.sessions.containsKey(view)) state.lastActive = view
            }
        }
        contentManager.addContentManagerListener(listener)
    }

    private fun focusTerminal(project: Project, view: TerminalView) {
        val tab = TerminalToolWindowTabsManager.getInstance(project)
            .tabs.firstOrNull { it.view === view }
            ?: return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
            ?: return

        project.service<TerminalSenderState>().lastActive = view
        toolWindow.activate({
            toolWindow.contentManager.setSelectedContent(tab.content, true)
            tab.view.preferredFocusableComponent.requestFocusInWindow()
        }, true)
    }

    private suspend fun startAgent(view: TerminalView, agent: AgentProfile) {
        awaitShellReady(view)
        if (agent.command.isBlank()) return
        view.createSendTextBuilder().shouldExecute().send(agent.command)
        delay(settings().state.agentStartupDelayMs.toLong())
    }

    private fun runInSession(
        project: Project,
        view: TerminalView,
        block: suspend (TerminalView) -> Unit,
    ) {
        val state = project.service<TerminalSenderState>()
        val session = state.sessions[view] ?: return
        state.scope.launch {
            session.ready.await()
            block(view)
        }
    }

    private suspend fun sendText(view: TerminalView, text: String, execute: Boolean) {
        val builder = view.createSendTextBuilder()
        if (execute) {
            builder.shouldExecute()
        } else {
            // Paste without submitting; bracketed mode keeps embedded newlines from
            // being typed as Enter keypresses in the shell.
            builder.useBracketedPasteMode()
        }
        builder.send(text)
    }

    private suspend fun sendPayload(view: TerminalView, payload: RelayPayload) {
        for (step in payload.steps) {
            sendText(view, step.text, step.execute)
            if (step.delayMsAfter > 0) {
                delay(step.delayMsAfter)
            }
        }
    }

    private suspend fun awaitShellReady(view: TerminalView) {
        // Wait for the shell to be ready so the first command isn't dropped.
        // Guarded with a timeout because shellIntegrationDeferred.await() may
        // never complete for shells without integration support (only Bash,
        // Zsh and PowerShell are supported) — after the timeout we send anyway.
        withTimeoutOrNull(5_000) { view.shellIntegrationDeferred.await() }
    }
}
