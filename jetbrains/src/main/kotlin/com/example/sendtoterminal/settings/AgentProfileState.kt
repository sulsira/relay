package com.example.sendtoterminal.settings

import com.example.sendtoterminal.format.AgentLaunchMode
import com.example.sendtoterminal.format.AgentReferenceMode
import com.example.sendtoterminal.OpenAgentActions
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.util.ui.ColumnInfo

class AgentProfileState : BaseState() {
    var name by string()
    var command by string()
    var referenceMode by string(AgentReferenceMode.AUTO.name)
    var launchMode by string(AgentLaunchMode.AUTO.name)
}

data class AgentProfile(
    var name: String,
    var command: String,
    var referenceMode: AgentReferenceMode = AgentReferenceMode.AUTO,
    var launchMode: AgentLaunchMode = AgentLaunchMode.AUTO,
)

class AgentNameColumn : ColumnInfo<AgentProfile, String>("Name") {
    override fun getTooltipText(): String = "Unique label. Used in the tab title Relay:<name>, to detect the agent kind, and in the Open <name> action."
    override fun valueOf(item: AgentProfile): String = item.name
    override fun setValue(item: AgentProfile, value: String) {
        item.name = value.trim()
    }
    override fun isCellEditable(item: AgentProfile): Boolean = true
}

class AgentCommandColumn : ColumnInfo<AgentProfile, String>("Command") {
    override fun getTooltipText(): String = "Shell command run once in the new tab to start the agent, for example claude."
    override fun valueOf(item: AgentProfile): String = item.command
    override fun setValue(item: AgentProfile, value: String) {
        item.command = value.trim()
    }
    override fun isCellEditable(item: AgentProfile): Boolean = true
}

class AgentReferenceModeColumn : ColumnInfo<AgentProfile, AgentReferenceMode>("Reference") {
    override fun getTooltipText(): String = "How file and selection references are written. Auto picks the format from the name or command."
    override fun valueOf(item: AgentProfile): AgentReferenceMode = item.referenceMode
    override fun setValue(item: AgentProfile, value: AgentReferenceMode) {
        item.referenceMode = value
    }
    override fun isCellEditable(item: AgentProfile): Boolean = true
}

class AgentLaunchModeColumn : ColumnInfo<AgentProfile, AgentLaunchMode>("Launch") {
    override fun getTooltipText(): String = "Only used for Goose: Interactive (TUI) or Plain (batch CLI). Auto reads the command."
    override fun valueOf(item: AgentProfile): AgentLaunchMode = item.launchMode
    override fun setValue(item: AgentProfile, value: AgentLaunchMode) {
        item.launchMode = value
    }
    override fun isCellEditable(item: AgentProfile): Boolean = true
}

/** Read-only view of the keymap binding for the agent's "Open <agent>" action. Double-click to set; use Clear Shortcut in the toolbar to remove. */
class AgentShortcutColumn : ColumnInfo<AgentProfile, String>("Shortcut") {
    override fun getTooltipText(): String = "Optional key that opens this agent. Double-click to set, use Clear Shortcut to remove."
    override fun valueOf(item: AgentProfile): String {
        val shortcuts = OpenAgentActions.shortcutsFor(item.name)
        if (shortcuts.isEmpty()) return ""
        return KeymapUtil.getShortcutsText(shortcuts)
    }
    override fun isCellEditable(item: AgentProfile): Boolean = false
}
