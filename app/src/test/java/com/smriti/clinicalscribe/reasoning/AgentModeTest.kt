package com.smriti.clinicalscribe.reasoning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentModeTest {
    @Test
    fun mockIsDefaultAgentMode() {
        assertEquals(AgentMode.MOCK, AgentConfig.DEFAULT_MODE)
        assertTrue(GemmaAgentFactory.create() is MockGemmaAgent)
    }
}
