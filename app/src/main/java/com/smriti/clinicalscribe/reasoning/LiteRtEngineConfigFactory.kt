package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EngineConfig

sealed class LiteRtEngineConfigPreparation {
    data class NotPrepared(
        val reason: String,
        val modelStatus: ModelStatus,
        val configConstructionAllowed: Boolean = false
    ) : LiteRtEngineConfigPreparation()

    data class Prepared(
        val modelPath: String,
        val backendLabel: String = "CPU",
        val engineConfig: EngineConfig,
        val configConstructionAllowed: Boolean = true,
        val reason: String = ENGINE_CONFIG_READY_REASON,
        val engineCreated: Boolean = false,
        val engineInitializationAttempted: Boolean = false,
        val conversationCreated: Boolean = false,
        val inferenceAttempted: Boolean = false
    ) : LiteRtEngineConfigPreparation()

    companion object {
        const val ENGINE_CONFIG_READY_REASON =
            "LiteRT-LM EngineConfig constructed; Engine initialization is manual-only."
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
            backendLabel = "CPU",
            engineConfig = EngineConfig(
                modelPath = modelStatus.expectedPath,
                backend = Backend.CPU()
            )
        )
    }
}
