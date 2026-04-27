package com.smriti.clinicalscribe.reasoning

class LiteRtGemmaTextClient(
    private val modelStatus: ModelStatus? = null
) : RealGemmaTextClient {
    val modelLoadAttempted: Boolean = false
    val engineInitializationAttempted: Boolean = false
    val inferenceAttempted: Boolean = false

    override suspend fun generateText(prompt: String): TextGenerationResult {
        return TextGenerationResult.Unavailable(
            status = buildString {
                append("LiteRT-LM client scaffold present; engine initialization intentionally disabled. ")
                append("No diagnosis generated. CHW confirmation required. ")
                append("Protocol citation required before recommendation.")
                modelStatus?.let { status ->
                    append(" Model status: ${status.proofLabel}.")
                }
            }
        )
    }

    fun apiSurfaceClassNames(): List<String> {
        return LITERT_API_SURFACE_CLASS_NAMES
    }

    fun apiSurfaceProbeStatus(): String {
        return "Direct LiteRT-LM imports are deferred: the current KAPT/JDK 17 path cannot read LiteRT-LM classfile version 65."
    }

    private companion object {
        val LITERT_API_SURFACE_CLASS_NAMES = listOf(
            "com.google.ai.edge.litertlm.Engine",
            "com.google.ai.edge.litertlm.EngineConfig",
            "com.google.ai.edge.litertlm.Backend",
            "com.google.ai.edge.litertlm.Content\$Text",
            "com.google.ai.edge.litertlm.Conversation"
        )
    }
}
