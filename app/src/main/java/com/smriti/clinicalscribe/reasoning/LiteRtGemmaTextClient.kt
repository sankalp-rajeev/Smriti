package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig

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
        return "Direct LiteRT-LM imports compile under JDK 21; engine initialization intentionally disabled."
    }

    @Suppress("UNUSED_PARAMETER")
    private fun compileOnlyApiSurfaceProbe(
        engine: Engine? = null,
        engineConfig: EngineConfig? = null,
        backend: Backend? = null,
        textContent: Content.Text? = null,
        conversation: Conversation? = null
    ) = Unit

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
