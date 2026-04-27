package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LiteRtGemmaTextClient(
    private val modelStatus: ModelStatus? = null,
    private val engineConfigFactory: LiteRtEngineConfigFactory = LiteRtEngineConfigFactory(),
    private val apiSurfaceProbe: LiteRtApiSurfaceProbe = LiteRtApiSurfaceProbe(),
    private val manualInferenceRunner: ManualTextInferenceRunner = RealManualTextInferenceRunner
) : RealGemmaTextClient {
    var modelLoadAttempted: Boolean = false
        private set
    var engineInitializationAttempted: Boolean = false
        private set
    var inferenceAttempted: Boolean = false
        private set
    var conversationCreated: Boolean = false
        private set

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

    suspend fun generateTextManual(
        prompt: String,
        allowManualTextInference: Boolean,
        timeoutMillis: Long = DEFAULT_MANUAL_TIMEOUT_MILLIS
    ): TextGenerationResult {
        if (!allowManualTextInference) {
            return TextGenerationResult.Unavailable(
                "Manual LiteRT-LM text inference skipped: allowManualTextInference=false."
            )
        }

        val status = modelStatus
            ?: return TextGenerationResult.Unavailable("Manual LiteRT-LM text inference unavailable: model status not provided.")

        if (status.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return TextGenerationResult.Unavailable("Manual LiteRT-LM text inference unavailable: model not found.")
        }

        val prepared = engineConfigFactory.prepare(status) as? LiteRtEngineConfigPreparation.Prepared
            ?: return TextGenerationResult.Unavailable("Manual LiteRT-LM text inference unavailable: EngineConfig not ready.")

        return try {
            val text = withTimeout(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    modelLoadAttempted = true
                    engineInitializationAttempted = true
                    conversationCreated = true
                    inferenceAttempted = true
                    manualInferenceRunner.generateText(prepared.engineConfig, prompt)
                }
            }
            TextGenerationResult.Success(text)
        } catch (_: TimeoutCancellationException) {
            TextGenerationResult.Failed("Manual LiteRT-LM text inference timed out after ${timeoutMillis}ms.")
        } catch (error: RuntimeException) {
            TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        }
    }

    fun interface ManualTextInferenceRunner {
        fun generateText(engineConfig: EngineConfig, prompt: String): String
    }

    private object RealManualTextInferenceRunner : ManualTextInferenceRunner {
        override fun generateText(engineConfig: EngineConfig, prompt: String): String {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    return conversation.sendMessage(prompt).extractText()
                }
            }
        }

        private fun com.google.ai.edge.litertlm.Message.extractText(): String {
            val text = contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString(separator = "\n") { it.text }
                .trim()
            if (text.isBlank()) {
                throw RuntimeException("LiteRT-LM response contained no text content.")
            }
            return text
        }
    }

    companion object {
        const val DEFAULT_MANUAL_TIMEOUT_MILLIS = 60_000L
    }
}
