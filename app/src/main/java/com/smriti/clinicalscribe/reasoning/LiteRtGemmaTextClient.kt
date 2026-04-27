package com.smriti.clinicalscribe.reasoning

class LiteRtGemmaTextClient(
    private val modelStatus: ModelStatus? = null,
    private val engineConfigFactory: LiteRtEngineConfigFactory = LiteRtEngineConfigFactory(),
    private val apiSurfaceProbe: LiteRtApiSurfaceProbe = LiteRtApiSurfaceProbe()
) : RealGemmaTextClient {
    val modelLoadAttempted: Boolean = false
    val engineInitializationAttempted: Boolean = false
    val inferenceAttempted: Boolean = false
    val conversationCreated: Boolean = false

    override suspend fun generateText(prompt: String): TextGenerationResult {
        val configStatus = modelStatus?.let { status ->
            when (val preparation = engineConfigFactory.prepare(status)) {
                is LiteRtEngineConfigPreparation.Prepared ->
                    " EngineConfig ready for ${preparation.backendLabel}; ${preparation.reason}"
                is LiteRtEngineConfigPreparation.NotPrepared -> " ${preparation.reason}"
            }
        }.orEmpty()
        return TextGenerationResult.Unavailable(
            status = buildString {
                append("LiteRT-LM client scaffold present; Engine initialization is manual-only and inference remains disabled. ")
                append("No diagnosis generated. CHW confirmation required. ")
                append("Protocol citation required before recommendation.")
                append(configStatus)
                modelStatus?.let { status ->
                    append(" Model status: ${status.proofLabel}.")
                }
            }
        )
    }

    fun apiSurfaceProbeStatus(): String {
        return "Direct LiteRT-LM API types compile: ${apiSurfaceProbe.verifiedTypeNames.joinToString()}."
    }
}
