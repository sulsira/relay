package com.example.sendtoterminal.format

enum class AgentReferenceMode(val label: String) {
    AUTO("Auto"),
    ADD("Add"),
    READ_ONLY("Read-only"),
    AT_MENTION("@ mention"),
    PLAIN("Plain"),
    ;

    companion object {
        fun fromStored(value: String?): AgentReferenceMode {
            if (value.isNullOrBlank()) return AUTO
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.label.equals(value, ignoreCase = true) }
                ?: AUTO
        }
    }
}
