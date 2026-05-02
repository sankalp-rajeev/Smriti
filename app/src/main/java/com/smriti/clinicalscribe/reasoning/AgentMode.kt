package com.smriti.clinicalscribe.reasoning

enum class AgentMode(val displayName: String) {
    MOCK("MockGemmaAgent"),
    REAL_GEMMA_REQUIRED("RealGemmaAgent"),
    REAL_GEMMA_EXPERIMENTAL("RealGemmaAgent experimental")
}
