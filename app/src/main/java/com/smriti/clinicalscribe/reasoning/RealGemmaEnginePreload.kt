package com.smriti.clinicalscribe.reasoning

enum class RealGemmaEnginePreloadState(val label: String) {
    UNAVAILABLE("Loads on demand"),
    LOADS_ON_DEMAND("Loads on demand"),
    PREPARING("Preparing"),
    READY("Ready"),
    FAILED("Failed")
}

sealed class RealGemmaPreloadResult {
    data object Ready : RealGemmaPreloadResult()
    data class Unavailable(val reason: String) : RealGemmaPreloadResult()
    data class Failed(val reason: String) : RealGemmaPreloadResult()
}

interface RealGemmaPreloadable {
    suspend fun preload(): RealGemmaPreloadResult
}
