package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig

sealed class LiteRtEngineInitializationResult {
    data object SkippedNotAllowed : LiteRtEngineInitializationResult()
    data object ModelNotFound : LiteRtEngineInitializationResult()
    data object ConfigNotReady : LiteRtEngineInitializationResult()
    data object InitializedAndClosed : LiteRtEngineInitializationResult()
    data class Failed(val reason: String) : LiteRtEngineInitializationResult()
}

class LiteRtEngineInitializationChecker(
    private val initializer: EngineInitializer = RealEngineInitializer
) {
    fun check(
        modelStatus: ModelStatus,
        configPreparation: LiteRtEngineConfigPreparation,
        allowManualEngineInitialization: Boolean
    ): LiteRtEngineInitializationResult {
        if (!allowManualEngineInitialization) {
            return LiteRtEngineInitializationResult.SkippedNotAllowed
        }

        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return LiteRtEngineInitializationResult.ModelNotFound
        }

        val prepared = configPreparation as? LiteRtEngineConfigPreparation.Prepared
            ?: return LiteRtEngineInitializationResult.ConfigNotReady

        return try {
            initializer.initializeAndClose(prepared.engineConfig)
            LiteRtEngineInitializationResult.InitializedAndClosed
        } catch (error: RuntimeException) {
            LiteRtEngineInitializationResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            LiteRtEngineInitializationResult.Failed(error.message ?: error::class.java.simpleName)
        }
    }

    fun interface EngineInitializer {
        fun initializeAndClose(engineConfig: EngineConfig)
    }

    private object RealEngineInitializer : EngineInitializer {
        override fun initializeAndClose(engineConfig: EngineConfig) {
            Engine(engineConfig).use { engine ->
                engine.initialize()
            }
        }
    }
}
