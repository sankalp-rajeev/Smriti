package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EngineConfig

sealed class LiteRtEngineConfigPreparation {
    data class NotPrepared(
        val reason: String,
        val modelStatus: ModelStatus
    ) : LiteRtEngineConfigPreparation()

    data class Prepared(
        val modelPath: String,
        val engineConfig: EngineConfig,
        val backendLabel: String,
        val engineCreated: Boolean = false,
        val engineInitializationAttempted: Boolean = false,
        val conversationCreated: Boolean = false,
        val inferenceAttempted: Boolean = false
    ) : LiteRtEngineConfigPreparation()
}

class LiteRtEngineConfigFactory {
    fun prepare(modelStatus: ModelStatus): LiteRtEngineConfigPreparation {
        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return LiteRtEngineConfigPreparation.NotPrepared(
                reason = "LiteRT-LM EngineConfig not prepared because the local model file was not found.",
                modelStatus = modelStatus
            )
        }

        val backend = Backend.CPU()
        return LiteRtEngineConfigPreparation.Prepared(
            modelPath = modelStatus.expectedPath,
            engineConfig = EngineConfig(
                modelPath = modelStatus.expectedPath,
                backend = backend
            ),
            backendLabel = backend.name
        )
    }
}
