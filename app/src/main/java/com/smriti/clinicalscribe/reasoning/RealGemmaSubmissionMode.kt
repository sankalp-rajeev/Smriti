package com.smriti.clinicalscribe.reasoning

data class RealGemmaSubmissionModeStatus(
    val buildTimeGateEnabled: Boolean,
    val localGateEnabled: Boolean,
    val modelStatus: ModelStatus
) {
    val isRequested: Boolean = buildTimeGateEnabled
    val isFullyActive: Boolean = buildTimeGateEnabled &&
        localGateEnabled &&
        modelStatus.kind == ModelStatusKind.FOUND_NOT_LOADED
    val usesRealGemmaVisitAgent: Boolean = isFullyActive
    val reasoningModeLabel: String = if (isFullyActive) {
        "RealGemmaAgent"
    } else {
        "MockGemmaAgent"
    }
    val submissionModeLabel: String = if (buildTimeGateEnabled) "ACTIVE" else "Disabled"
    val realGemmaTextModeLabel: String = if (isFullyActive) "ACTIVE" else "Disabled"
    val inferenceStatusLabel: String = if (isFullyActive) {
        "Enabled; on-device RealGemma text reasoning"
    } else {
        "Disabled"
    }
    val gateStatusLabel: String = "Submission build flag: ${gateLabel(buildTimeGateEnabled)}; local gate: ${gateLabel(localGateEnabled)}"
    val warning: String? = when {
        isFullyActive -> "Submission RealGemma text mode. CHW review and confirmation are still required before saving."
        buildTimeGateEnabled -> "Submission mode requested, but RealGemma gates are incomplete; normal MockGemmaAgent behavior remains active."
        else -> null
    }

    private fun gateLabel(enabled: Boolean): String = if (enabled) "enabled" else "disabled"
}

object RealGemmaSubmissionMode {
    fun evaluate(
        buildTimeGateEnabled: Boolean,
        localGateEnabled: Boolean,
        modelStatus: ModelStatus
    ): RealGemmaSubmissionModeStatus {
        return RealGemmaSubmissionModeStatus(
            buildTimeGateEnabled = buildTimeGateEnabled,
            localGateEnabled = localGateEnabled,
            modelStatus = modelStatus
        )
    }
}

object RealGemmaUnavailableResult {
    const val RETRY_MESSAGE = "On-device reasoning unavailable — please retry."

    fun isUnavailable(result: VisitReasoningResult): Boolean {
        val text = listOf(
            result.structuredNote,
            result.suggestedFollowUp,
            result.clarificationPrompt.orEmpty()
        ).joinToString(separator = "\n").lowercase()
        return result.uncertain && (
                "real gemma path unavailable" in text ||
                "real gemma output rejected" in text ||
                "text generation failed" in text ||
                "model unavailable" in text ||
                "manual text inference" in text ||
                "model" in text && "not" in text && "found" in text
            )
    }
}
