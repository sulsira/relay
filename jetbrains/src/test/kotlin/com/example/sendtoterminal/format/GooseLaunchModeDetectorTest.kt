package com.example.sendtoterminal.format

import org.junit.Assert.assertEquals
import org.junit.Test

class GooseLaunchModeDetectorTest {

    @Test
    fun `detects interactive goose commands`() {
        assertEquals(AgentLaunchMode.INTERACTIVE, GooseLaunchModeDetector.detectFromCommand("goose"))
        assertEquals(AgentLaunchMode.INTERACTIVE, GooseLaunchModeDetector.detectFromCommand("goose session"))
        assertEquals(AgentLaunchMode.INTERACTIVE, GooseLaunchModeDetector.detectFromCommand("goose run -s"))
        assertEquals(AgentLaunchMode.INTERACTIVE, GooseLaunchModeDetector.detectFromCommand("goose run --interactive"))
        assertEquals(AgentLaunchMode.INTERACTIVE, GooseLaunchModeDetector.detectFromCommand("open -a Goose"))
    }

    @Test
    fun `detects plain goose commands`() {
        assertEquals(AgentLaunchMode.PLAIN, GooseLaunchModeDetector.detectFromCommand("goose run"))
        assertEquals(AgentLaunchMode.PLAIN, GooseLaunchModeDetector.detectFromCommand("goose run -i task.md"))
        assertEquals(AgentLaunchMode.PLAIN, GooseLaunchModeDetector.detectFromCommand("goose serve"))
        assertEquals(AgentLaunchMode.PLAIN, GooseLaunchModeDetector.detectFromCommand("goose configure"))
    }
}
