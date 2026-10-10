package com.example.sendtoterminal.format

sealed class RelayPayload {
    abstract val steps: List<Step>

    data class Single(val step: Step) : RelayPayload() {
        override val steps: List<Step> = listOf(step)
    }

    data class Sequence(override val steps: List<Step>) : RelayPayload()

    data class Step(
        val text: String,
        val execute: Boolean = false,
        val delayMsAfter: Long = 0,
    )

    companion object {
        fun message(text: String, execute: Boolean = false): RelayPayload =
            Single(Step(text, execute))

        fun sequence(vararg steps: Step): RelayPayload = Sequence(steps.toList())
    }
}
