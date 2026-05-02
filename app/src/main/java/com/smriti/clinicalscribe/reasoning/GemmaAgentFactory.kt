package com.smriti.clinicalscribe.reasoning

object GemmaAgentFactory {
    fun create(mode: AgentMode = AgentConfig.DEFAULT_MODE): GemmaAgent {
        return when (mode) {
            AgentMode.MOCK -> MockGemmaAgent()
            AgentMode.REAL_GEMMA_REQUIRED -> RealGemmaAgent()
            AgentMode.REAL_GEMMA_EXPERIMENTAL -> RealGemmaAgent()
        }
    }
}
