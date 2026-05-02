package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
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
                    SmritiLatencyLogger.mark("realGemmaGenerateCallStart")
                    modelLoadAttempted = true
                    engineInitializationAttempted = true
                    conversationCreated = true
                    inferenceAttempted = true
                    val text = manualInferenceRunner.generateText(prepared.engineConfig, prompt)
                    SmritiLatencyLogger.mark("realGemmaGenerateCallEnd")
                    text
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

    suspend fun preloadManual(
        allowManualTextInference: Boolean,
        timeoutMillis: Long = DEFAULT_MANUAL_TIMEOUT_MILLIS
    ): RealGemmaPreloadResult {
        if (!allowManualTextInference) {
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload skipped: allowManualTextInference=false.")
        }

        val status = modelStatus
            ?: return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload unavailable: model status not provided.")

        if (status.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload unavailable: model not found.")
        }

        val prepared = engineConfigFactory.prepare(status) as? LiteRtEngineConfigPreparation.Prepared
            ?: return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload unavailable: EngineConfig not ready.")

        return try {
            withTimeout(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    val duration = kotlin.system.measureTimeMillis {
                        modelLoadAttempted = true
                        engineInitializationAttempted = true
                        conversationCreated = true
                        manualInferenceRunner.preload(prepared.engineConfig)
                    }
                    SmritiLatencyLogger.log("realGemmaEnginePreloadInit", duration)
                }
            }
            RealGemmaPreloadResult.Ready
        } catch (_: TimeoutCancellationException) {
            RealGemmaPreloadResult.Failed("Manual LiteRT-LM preload timed out after ${timeoutMillis}ms.")
        } catch (error: RuntimeException) {
            RealGemmaPreloadResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            RealGemmaPreloadResult.Failed(error.message ?: error::class.java.simpleName)
        }
    }

    fun interface ManualTextInferenceRunner {
        fun generateText(engineConfig: EngineConfig, prompt: String): String

        fun preload(engineConfig: EngineConfig) {
            // Default test runners can remain generation-only.
        }
    }

    private object RealManualTextInferenceRunner : ManualTextInferenceRunner {
        private var cachedEngine: Engine? = null
        private var cachedConversation: Conversation? = null
        private var cachedModelPath: String? = null

        @Synchronized
        override fun preload(engineConfig: EngineConfig) {
            ensureConversation(engineConfig)
        }

        @Synchronized
        override fun generateText(engineConfig: EngineConfig, prompt: String): String {
            val conversation = ensureConversation(engineConfig)
            return conversation.sendMessage(prompt).extractText()
        }

        private fun ensureConversation(engineConfig: EngineConfig): Conversation {
            val modelPath = engineConfig.modelPath
            val existing = cachedConversation
            if (existing != null && cachedModelPath == modelPath) {
                return existing
            }

            closeCached()
            val engine = Engine(engineConfig)
            engine.initialize()
            val conversation = engine.createConversation()
            cachedEngine = engine
            cachedConversation = conversation
            cachedModelPath = modelPath
            return conversation
        }

        private fun closeCached() {
            runCatching { cachedConversation?.close() }
            runCatching { cachedEngine?.close() }
            cachedConversation = null
            cachedEngine = null
            cachedModelPath = null
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
