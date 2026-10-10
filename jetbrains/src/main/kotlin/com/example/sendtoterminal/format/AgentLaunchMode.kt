package com.example.sendtoterminal.format

enum class AgentLaunchMode(val label: String) {
    AUTO("Auto"),
    INTERACTIVE("Interactive"),
    PLAIN("Plain"),
    ;

    companion object {
        fun fromStored(value: String?): AgentLaunchMode {
            if (value.isNullOrBlank()) return AUTO
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.label.equals(value, ignoreCase = true) }
                ?: AUTO
        }
    }
}
