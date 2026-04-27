package com.smriti.clinicalscribe.reasoning

enum class RealGemmaReadinessStatus {
    MOCK_ACTIVE,
    EXPERIMENTAL_DISABLED,
    MODEL_NOT_FOUND,
    MODEL_FOUND_CONFIG_READY_ENGINE_DISABLED,
    READY_FOR_MANUAL_ENGINE_LOAD_TEST
}

data class RealGemmaReadinessSummary(
    val status: RealGemmaReadinessStatus,
    val judgeLabel: String,
    val developerReason: String,
    val modelLoadingAllowed: Boolean,
    val inferenceAllowed: Boolean,
    val demoSafe: Boolean,
    val engineCreated: Boolean,
    val engineInitializationAttempted: Boolean,
    val conversationCreated: Boolean,
    val sendMessageAttempted: Boolean
)

class RealGemmaReadinessEvaluator {
    fun evaluate(
        agentMode: AgentMode,
        modelStatus: ModelStatus,
        engineConfigPreparation: LiteRtEngineConfigPreparation = LiteRtEngineConfigFactory().prepare(modelStatus),
        liteRtTextClientDisabled: Boolean = true,
        engineInitializationAllowed: Boolean = false,
        inferenceAllowed: Boolean = false
    ): RealGemmaReadinessSummary {
        if (agentMode == AgentMode.MOCK) {
            return summary(
                status = RealGemmaReadinessStatus.MOCK_ACTIVE,
                judgeLabel = "Mock active",
                developerReason = "Normal app mode is MOCK; experimental RealGemma is not active.",
                engineConfigPreparation = engineConfigPreparation
            )
        }

        if (!liteRtTextClientDisabled || engineInitializationAllowed || inferenceAllowed) {
            return summary(
                status = RealGemmaReadinessStatus.EXPERIMENTAL_DISABLED,
                judgeLabel = "Experimental disabled",
                developerReason = "Safety gate blocks RealGemma runtime activation; engine startup and inference remain disabled.",
                engineConfigPreparation = engineConfigPreparation
            )
        }

        if (modelStatus.kind == ModelStatusKind.NOT_FOUND) {
            return summary(
                status = RealGemmaReadinessStatus.MODEL_NOT_FOUND,
                judgeLabel = "Model not found",
                developerReason = "Experimental mode was selected, but the expected local model file is missing.",
                engineConfigPreparation = engineConfigPreparation
            )
        }

        return when (engineConfigPreparation) {
            is LiteRtEngineConfigPreparation.Prepared -> summary(
                status = RealGemmaReadinessStatus.MODEL_FOUND_CONFIG_READY_ENGINE_DISABLED,
                judgeLabel = "Model found, engine disabled",
                developerReason = "Model file is present and a CPU EngineConfig exists. Engine initialization is manual-only, and inference remains disabled.",
                engineConfigPreparation = engineConfigPreparation
            )
            is LiteRtEngineConfigPreparation.NotPrepared -> summary(
                status = RealGemmaReadinessStatus.EXPERIMENTAL_DISABLED,
                judgeLabel = "Experimental disabled",
                developerReason = "Experimental RealGemma remains disabled because an EngineConfig plan is not ready.",
                engineConfigPreparation = engineConfigPreparation
            )
        }
    }

    private fun summary(
        status: RealGemmaReadinessStatus,
        judgeLabel: String,
        developerReason: String,
        engineConfigPreparation: LiteRtEngineConfigPreparation
    ): RealGemmaReadinessSummary {
        val prepared = engineConfigPreparation as? LiteRtEngineConfigPreparation.Prepared
        return RealGemmaReadinessSummary(
            status = status,
            judgeLabel = judgeLabel,
            developerReason = developerReason,
            modelLoadingAllowed = false,
            inferenceAllowed = false,
            demoSafe = true,
            engineCreated = prepared?.engineCreated ?: false,
            engineInitializationAttempted = prepared?.engineInitializationAttempted ?: false,
            conversationCreated = prepared?.conversationCreated ?: false,
            sendMessageAttempted = prepared?.inferenceAttempted ?: false
        )
    }
}
