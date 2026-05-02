package com.smriti.clinicalscribe.reasoning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentModeTest {
    @Test
    fun realGemmaRequiredIsDefaultAgentMode() {
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, AgentConfig.DEFAULT_MODE)
        assertTrue(GemmaAgentFactory.create() is RealGemmaAgent)
    }

    @Test
    fun mockCanStillBeCreatedOnlyAsExplicitFixture() {
        assertTrue(GemmaAgentFactory.create(AgentMode.MOCK) is MockGemmaAgent)
    }
}
