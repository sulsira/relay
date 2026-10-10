package com.example.sendtoterminal.settings

import com.example.sendtoterminal.OpenAgentActions
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import javax.swing.JComponent

class SendToTerminalSettingsConfigurable : Configurable {

    private var panel: SendToTerminalSettingsPanel? = null

    override fun getDisplayName(): String = "Relay"

    override fun createComponent(): JComponent {
        panel = SendToTerminalSettingsPanel()
        return panel!!.getComponent()
    }

    override fun isModified(): Boolean = panel?.isModified() == true

    override fun apply() {
        try {
            panel?.apply()
            OpenAgentActions.sync(service<SendToTerminalSettings>().agents())
        } catch (e: IllegalArgumentException) {
            throw ConfigurationException(e.message)
        }
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}
