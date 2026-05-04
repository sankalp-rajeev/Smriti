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
    val usesRealGemmaVisitAgent: Boolean = true
    val reasoningModeLabel: String = "RealGemmaAgent"
    val submissionModeLabel: String = if (buildTimeGateEnabled) "Required" else "Required; build flag missing"
    val realGemmaTextModeLabel: String = if (isFullyActive) "Active" else "Setup required"
    val inferenceStatusLabel: String = if (isFullyActive) {
        "Enabled; on-device RealGemma text reasoning"
    } else {
        "Unavailable/setup required"
    }
    val gateStatusLabel: String = "Submission build flag: ${gateLabel(buildTimeGateEnabled)}; local gate: ${gateLabel(localGateEnabled)}"
    val warning: String? = when {
        isFullyActive -> "RealGemma text reasoning active. CHW review and confirmation are required before saving."
        else -> "RealGemma setup required; no mock clinical output will be shown."
    }

    private fun gateLabel(enabled: Boolean): String = if (enabled) "enabled" else "missing"
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
    const val RETRY_MESSAGE = "On-device RealGemma reasoning unavailable — please retry."

    fun retryMessageFor(result: VisitReasoningResult): String {
        val reason = result.clarificationPrompt
            ?.takeIf { it.isNotBlank() }
            ?: result.structuredNote
                .lineSequence()
                .firstOrNull { it.contains("RealGemma", ignoreCase = true) || it.contains("Real Gemma", ignoreCase = true) }
        return if (reason.isNullOrBlank()) {
            RETRY_MESSAGE
        } else {
            "$RETRY_MESSAGE $reason"
        }
    }

    fun isUnavailable(result: VisitReasoningResult): Boolean {
        val text = listOf(
            result.structuredNote,
            result.suggestedFollowUp,
            result.clarificationPrompt.orEmpty()
        ).joinToString(separator = "\n").lowercase()
        return result.uncertain && (
            "realgemma required setup incomplete" in text ||
                "realgemma reasoning is unavailable" in text ||
                "on-device reasoning unavailable" in text ||
                "realgemma output was rejected" in text ||
                "real gemma path unavailable" in text ||
                "real gemma output rejected" in text ||
                "text generation failed" in text ||
                "model unavailable" in text ||
                "manual text inference" in text ||
                "smriti is already preparing a note" in text ||
                "model" in text && "not" in text && "found" in text
            )
    }
}
