package com.smriti.clinicalscribe.reasoning

sealed class LiteRtEngineConfigPreparation {
    data class NotPrepared(
        val reason: String,
        val modelStatus: ModelStatus,
        val configConstructionAllowed: Boolean = false
    ) : LiteRtEngineConfigPreparation()

    data class Prepared(
        val modelPath: String,
        val backendLabel: String = "CPU",
        val configConstructionAllowed: Boolean = false,
        val reason: String = TYPE_CHECK_PASSED_REASON,
        val engineCreated: Boolean = false,
        val engineInitializationAttempted: Boolean = false,
        val conversationCreated: Boolean = false,
        val inferenceAttempted: Boolean = false
    ) : LiteRtEngineConfigPreparation()

    companion object {
        const val TYPE_CHECK_PASSED_REASON =
            "LiteRT-LM EngineConfig type check passed after KSP migration; Engine disabled."
    }
}

class LiteRtEngineConfigFactory {
    fun prepare(modelStatus: ModelStatus): LiteRtEngineConfigPreparation {
        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return LiteRtEngineConfigPreparation.NotPrepared(
                reason = "LiteRT-LM EngineConfig not prepared because the local model file was not found.",
                modelStatus = modelStatus
            )
        }

        return LiteRtEngineConfigPreparation.Prepared(
            modelPath = modelStatus.expectedPath,
            backendLabel = "CPU"
        )
    }
}
