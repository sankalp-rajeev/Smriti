package com.smriti.clinicalscribe.reasoning

import java.io.File

data class RealGemmaDeveloperModeStatus(
    val buildTimeGateEnabled: Boolean,
    val localGateEnabled: Boolean,
    val modelStatus: ModelStatus
) {
    val developerModeRequested: Boolean = buildTimeGateEnabled && localGateEnabled
    val activeAgentMode: AgentMode = if (developerModeRequested) {
        AgentMode.REAL_GEMMA_EXPERIMENTAL
    } else {
        AgentMode.MOCK
    }
    val usesRealGemmaVisitAgent: Boolean = developerModeRequested
    val inferenceEnabled: Boolean = developerModeRequested &&
        modelStatus.kind == ModelStatusKind.FOUND_NOT_LOADED
    val reasoningModeLabel: String = if (developerModeRequested) {
        "RealGemmaAgent / Developer-only / Experimental"
    } else {
        "MockGemmaAgent"
    }
    val inferenceStatusLabel: String = when {
        inferenceEnabled -> "Enabled for developer text mode; CPU backend"
        developerModeRequested -> "Disabled; local model missing or not ready"
        else -> "Disabled by default; manual-only"
    }
    val gateStatusLabel: String = "Build gate: ${gateLabel(buildTimeGateEnabled)}; local gate: ${gateLabel(localGateEnabled)}"
    val developerWarning: String? = if (developerModeRequested) {
        "Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving."
    } else {
        null
    }

    private fun gateLabel(enabled: Boolean): String = if (enabled) "enabled" else "disabled"
}

object RealGemmaDeveloperMode {
    const val LOCAL_GATE_RELATIVE_PATH = "dev/enable_real_gemma_text_mode"

    fun localGateFile(filesDir: File): File {
        return File(filesDir, LOCAL_GATE_RELATIVE_PATH)
    }

    fun isLocalGateEnabled(filesDir: File): Boolean {
        return localGateFile(filesDir).isFile
    }

    fun evaluate(
        buildTimeGateEnabled: Boolean,
        localGateEnabled: Boolean,
        modelStatus: ModelStatus
    ): RealGemmaDeveloperModeStatus {
        return RealGemmaDeveloperModeStatus(
            buildTimeGateEnabled = buildTimeGateEnabled,
            localGateEnabled = localGateEnabled,
            modelStatus = modelStatus
        )
    }
}

object RealGemmaDeveloperAgentFactory {
    fun createVisitAgent(
        status: RealGemmaDeveloperModeStatus,
        modelStatus: ModelStatus
    ): GemmaAgent {
        return if (status.usesRealGemmaVisitAgent) {
            RealGemmaAgent(textClient = RealGemmaDeveloperTextClient(modelStatus))
        } else {
            GemmaAgentFactory.create(AgentConfig.DEFAULT_MODE)
        }
    }
}

class RealGemmaDeveloperTextClient(
    private val liteRtClient: LiteRtGemmaTextClient,
    private val timeoutMillis: Long = LiteRtGemmaTextClient.DEFAULT_MANUAL_TIMEOUT_MILLIS
) : RealGemmaTextClient {
    constructor(
        modelStatus: ModelStatus,
        timeoutMillis: Long = LiteRtGemmaTextClient.DEFAULT_MANUAL_TIMEOUT_MILLIS
    ) : this(
        liteRtClient = LiteRtGemmaTextClient(modelStatus = modelStatus),
        timeoutMillis = timeoutMillis
    )

    override suspend fun generateText(prompt: String): TextGenerationResult {
        return liteRtClient.generateTextManual(
            prompt = prompt,
            allowManualTextInference = true,
            timeoutMillis = timeoutMillis
        )
    }
}
