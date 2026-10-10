package com.example.sendtoterminal.settings

import com.example.sendtoterminal.OpenAgentActions
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.impl.ShortcutRestrictions
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.example.sendtoterminal.format.AgentLaunchMode
import com.example.sendtoterminal.format.AgentReferenceMode
import com.example.sendtoterminal.SendToTerminalActionIds
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultCellEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener

class SendToTerminalSettingsPanel {

    private val agentDelayField = JBTextField()
    private val defaultAgentCombo = JComboBox<String>()
    private val shortcutLabels = mutableMapOf<String, JBLabel>()

    private val agentsModel = ListTableModel(
        arrayOf(AgentNameColumn(), AgentCommandColumn(), AgentReferenceModeColumn(), AgentLaunchModeColumn(), AgentShortcutColumn()),
        mutableListOf<AgentProfile>(),
    )
    private val agentsTable = TableView<AgentProfile>(agentsModel).apply {
        emptyText.appendText("No agents configured")
        component.setDefaultEditor(
            AgentReferenceMode::class.java,
            DefaultCellEditor(JComboBox(AgentReferenceMode.entries.toTypedArray())),
        )
        component.setDefaultEditor(
            AgentLaunchMode::class.java,
            DefaultCellEditor(JComboBox(AgentLaunchMode.entries.toTypedArray())),
        )
    }

    private val agentsPanel: JPanel = ToolbarDecorator.createDecorator(agentsTable.component)
        .setAddAction {
            agentsModel.addRow(AgentProfile(nextAgentName(), ""))
            refreshDefaultAgentCombo()
        }
        .setRemoveAction {
            val selected = agentsTable.selectedObject ?: return@setRemoveAction
            val index = agentsModel.items.indexOf(selected)
            if (index >= 0) {
                agentsModel.removeRow(index)
                refreshDefaultAgentCombo()
            }
        }
        .setRemoveActionUpdater { agentsModel.rowCount > 1 }
        .addExtraAction(clearShortcutButton())
        .createPanel()

    private val shortcutRows = listOf(
        ShortcutRow(SendToTerminalActionIds.OPEN, "Open Relay"),
        ShortcutRow(SendToTerminalActionIds.FILE_PATH, "Send file path"),
        ShortcutRow(SendToTerminalActionIds.SELECTION, "Send selection"),
    )

    private val root: DialogPanel = panel {
        group("Agents") {
            row {
                cell(agentsPanel)
                    .align(AlignX.FILL)
                    .comment(
                        "Reference overrides Auto. Launch detects Goose CLI vs interactive TUI (Auto uses the command). " +
                            "Shortcut is optional: double-click it to bind a key that opens that agent.",
                    )
            }
            row("Default agent:") {
                cell(defaultAgentCombo)
                    .comment("Switch quickly from the editor menu via Relay → Default Agent.")
            }
            row("Startup delay (ms):") {
                cell(agentDelayField)
                    .align(AlignX.FILL)
                    .comment("Wait time after the agent command before pasting follow-up input.")
            }
        }

        group("Keyboard shortcuts") {
            row {
                comment(
                    "Edit shortcuts below or open the full Keymap page. Changes apply to the active keymap.",
                )
            }
            shortcutRows.forEach { shortcutRow ->
                row(shortcutRow.label + ":") {
                    cell(createShortcutEditor(shortcutRow))
                        .align(AlignX.FILL)
                }
            }
            row {
                button("Open Keymap settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(
                        null,
                        KeymapPanel::class.java,
                    ) { keymapPanel ->
                        keymapPanel.selectAction(SendToTerminalActionIds.OPEN)
                    }
                }
            }
        }
    }

    fun getComponent(): JComponent = root

    fun isModified(): Boolean {
        val settings = service<SendToTerminalSettings>()
        settings.ensureInitialized()
        return agentsSnapshot() != settings.agents() ||
            selectedDefaultAgentName() != settings.defaultAgent().name ||
            agentDelayField.text.trim().toIntOrNull() != settings.state.agentStartupDelayMs
    }

    fun reset() {
        val settings = service<SendToTerminalSettings>()
        settings.ensureInitialized()
        agentsModel.items = settings.agents().map {
            AgentProfile(it.name, it.command, it.referenceMode, it.launchMode)
        }.toMutableList()
        agentDelayField.text = settings.state.agentStartupDelayMs.toString()
        refreshDefaultAgentCombo(settings.defaultAgent().name)
        refreshShortcutLabels()
    }

    fun apply() {
        val agents = agentsSnapshot()
        validateAgents(agents)

        val settings = service<SendToTerminalSettings>()
        val defaultName = selectedDefaultAgentName()
            ?: throw IllegalArgumentException("Select a default agent.")
        settings.replaceAgents(agents, defaultName)
        settings.state.agentStartupDelayMs = agentDelayField.text.trim().toIntOrNull()
            ?: SendToTerminalSettings.DEFAULT_AGENT_STARTUP_DELAY_MS
    }

    private fun validateAgents(agents: List<AgentProfile>) {
        require(agents.isNotEmpty()) { "At least one agent is required." }
        require(agents.all { it.name.isNotBlank() && it.command.isNotBlank() }) {
            "Each agent needs a name and command."
        }
        require(agents.map { it.name }.distinct().size == agents.size) {
            "Agent names must be unique."
        }
    }

    private fun agentsSnapshot(): List<AgentProfile> =
        agentsModel.items.map {
            AgentProfile(it.name.trim(), it.command.trim(), it.referenceMode, it.launchMode)
        }

    private fun selectedDefaultAgentName(): String? =
        defaultAgentCombo.selectedItem as? String

    private fun refreshDefaultAgentCombo(preferredDefault: String? = selectedDefaultAgentName()) {
        val names = agentsSnapshot().map { it.name }
        val model = DefaultComboBoxModel(names.toTypedArray())
        defaultAgentCombo.model = model
        val selection = preferredDefault?.takeIf { it in names } ?: names.firstOrNull()
        if (selection != null) {
            defaultAgentCombo.selectedItem = selection
        }
    }

    private fun createShortcutEditor(shortcutRow: ShortcutRow): JComponent {
        val panel = JPanel(BorderLayout(JBUI.scale(8), 0))
        val shortcutsLabel = JBLabel()
        shortcutLabels[shortcutRow.actionId] = shortcutsLabel
        panel.add(shortcutsLabel, BorderLayout.CENTER)

        val editButton = JButton("Edit…")
        editButton.addActionListener {
            KeymapPanel.addKeyboardShortcut(
                shortcutRow.actionId,
                ShortcutRestrictions.NO_RESTRICTIONS,
                KeymapManager.getInstance().activeKeymap,
                editButton,
            )
            refreshShortcutLabels()
        }
        panel.add(editButton, BorderLayout.EAST)
        return panel
    }

    private fun refreshShortcutLabels() {
        val keymap = KeymapManager.getInstance().activeKeymap
        shortcutRows.forEach { row ->
            val label = shortcutLabels[row.actionId] ?: return@forEach
            val shortcuts = keymap.getShortcuts(row.actionId)
            label.text = if (shortcuts.isEmpty()) {
                "No shortcut assigned"
            } else {
                KeymapUtil.getShortcutsText(shortcuts)
            }
        }
    }

    private fun clearShortcutButton(): AnActionButton =
        object : AnActionButton("Clear Shortcut", "Remove the shortcut of the selected agent", AllIcons.Actions.Cancel) {
            override fun actionPerformed(e: AnActionEvent) {
                val agent = agentsTable.selectedObject ?: return
                OpenAgentActions.clearShortcuts(agent.name)
                agentsTable.repaint()
            }

            override fun updateButton(e: AnActionEvent) {
                val agent = agentsTable.selectedObject
                e.presentation.isEnabled = agent != null && OpenAgentActions.shortcutsFor(agent.name).isNotEmpty()
            }
        }

    private fun editAgentShortcut(row: Int) {
        val agent = agentsTable.getRow(row) ?: return
        val actionId = OpenAgentActions.idFor(agent.name)
        val registered = ActionManager.getInstance().getAction(actionId) != null
        if (!registered) {
            Messages.showInfoMessage(root, "Click Apply to save this agent, then assign its shortcut.", "Relay")
            return
        }
        KeymapPanel.addKeyboardShortcut(
            actionId,
            ShortcutRestrictions.NO_RESTRICTIONS,
            KeymapManager.getInstance().activeKeymap,
            agentsTable,
        )
        agentsTable.repaint()
    }

    private fun nextAgentName(): String {
        val existing = agentsModel.items.map(AgentProfile::name).toSet()
        var index = 1
        var candidate = "agent"
        while (candidate in existing) {
            candidate = "agent$index"
            index++
        }
        return candidate
    }

    private companion object {
        const val SHORTCUT_COLUMN_INDEX = 4
    }

    private data class ShortcutRow(val actionId: String, val label: String)

    init {
        agentsTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val column = agentsTable.columnAtPoint(e.point)
                if (column != SHORTCUT_COLUMN_INDEX) return
                val row = agentsTable.rowAtPoint(e.point)
                if (row < 0) return
                editAgentShortcut(row)
            }
        })
        agentsModel.addTableModelListener(TableModelListener {
            if (it.type == TableModelEvent.UPDATE || it.type == TableModelEvent.INSERT || it.type == TableModelEvent.DELETE) {
                refreshDefaultAgentCombo()
            }
        })
        reset()
    }
}
