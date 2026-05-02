package com.smriti.clinicalscribe.reasoning

data class RealGemmaRequiredModeStatus(
    val buildTimeGateEnabled: Boolean,
    val localGateEnabled: Boolean,
    val modelStatus: ModelStatus
) {
    val inferenceEnabled: Boolean = buildTimeGateEnabled &&
        localGateEnabled &&
        modelStatus.kind == ModelStatusKind.FOUND_NOT_LOADED

    val reasoningModeLabel: String = "RealGemmaAgent"
    val textModeLabel: String = if (inferenceEnabled) "Active" else "Setup required"
    val submissionModeLabel: String = if (buildTimeGateEnabled) "Required" else "Required; build flag missing"
    val gateStatusLabel: String =
        "Submission build flag: ${gateLabel(buildTimeGateEnabled)}; local gate: ${gateLabel(localGateEnabled)}"
    val readinessLabel: String = if (inferenceEnabled) {
        "RealGemma text reasoning active"
    } else {
        "RealGemma setup required"
    }
    val inferenceStatusLabel: String = if (inferenceEnabled) {
        "Enabled; on-device RealGemma text reasoning"
    } else {
        "Unavailable/setup required: $unavailableReason"
    }
    val warning: String? = if (inferenceEnabled) {
        "RealGemma text reasoning active. CHW review and confirmation are required before saving."
    } else {
        unavailableReason
    }

    val unavailableReason: String
        get() = buildList {
            if (!buildTimeGateEnabled) add("build with -Psmriti.realGemmaSubmissionMode=true")
            if (!localGateEnabled) add("create app-private files/dev/enable_real_gemma_text_mode")
            if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
                add("sideload ${LiteRtModelPaths.GEMMA_E2B_MODEL_FILE_NAME} to app-private files/models")
            }
        }.joinToString(separator = "; ")
            .ifBlank { "RealGemma inference is not ready." }

    private fun gateLabel(enabled: Boolean): String = if (enabled) "enabled" else "missing"
}

object RealGemmaRequiredMode {
    fun evaluate(
        buildTimeGateEnabled: Boolean,
        localGateEnabled: Boolean,
        modelStatus: ModelStatus
    ): RealGemmaRequiredModeStatus {
        return RealGemmaRequiredModeStatus(
            buildTimeGateEnabled = buildTimeGateEnabled,
            localGateEnabled = localGateEnabled,
            modelStatus = modelStatus
        )
    }
}

object RealGemmaRequiredAgentFactory {
    fun createVisitAgent(
        status: RealGemmaRequiredModeStatus,
        modelStatus: ModelStatus,
        sharedTextClient: RealGemmaTextClient? = null
    ): RealGemmaAgent {
        val textClient = if (status.inferenceEnabled) {
            sharedTextClient ?: RealGemmaDeveloperTextClient(modelStatus)
        } else {
            UnavailableGemmaTextClient(
                status = "RealGemma required setup incomplete: ${status.unavailableReason}"
            )
        }
        return RealGemmaAgent(textClient = textClient)
    }

    fun createSupervisorPriorityGenerator(
        status: RealGemmaRequiredModeStatus,
        modelStatus: ModelStatus,
        sharedTextClient: RealGemmaTextClient? = null
    ): SupervisorPriorityQueueGenerator {
        val textClient = if (status.inferenceEnabled) {
            sharedTextClient ?: RealGemmaDeveloperTextClient(modelStatus)
        } else {
            UnavailableGemmaTextClient(
                status = "RealGemma supervisor setup incomplete: ${status.unavailableReason}"
            )
        }
        return SupervisorPriorityQueueGenerator(textClient = textClient)
    }
}
