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
    private val manualInferenceRunner: ManualTextInferenceRunner = RealManualTextInferenceRunner,
    private val sentinelExists: Boolean? = null
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
        timeoutMillis: Long = DEFAULT_MANUAL_TIMEOUT_MILLIS,
        requestType: RealGemmaRequestType = RealGemmaRequestType.MANUAL_TEST
    ): TextGenerationResult {
        val lease = RealGemmaInferenceGate.tryAcquire(requestType, requestDiagnostics(requestType))
            ?: return TextGenerationResult.Unavailable(RealGemmaInferenceGate.BUSY_MESSAGE)
        if (!allowManualTextInference) {
            lease.release()
            return TextGenerationResult.Unavailable(
                "Manual LiteRT-LM text inference skipped: allowManualTextInference=false."
            )
        }

        val status = modelStatus
        if (status == null) {
            lease.release()
            return TextGenerationResult.Unavailable("Manual LiteRT-LM text inference unavailable: model status not provided.")
        }

        if (status.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            lease.release()
            return TextGenerationResult.Unavailable("Manual LiteRT-LM text inference unavailable: model not found.")
        }

        val prepared = engineConfigFactory.prepare(status) as? LiteRtEngineConfigPreparation.Prepared
        if (prepared == null) {
            lease.release()
            return TextGenerationResult.Unavailable("Manual LiteRT-LM text inference unavailable: EngineConfig not ready.")
        }

        return try {
            var generationDuration = 0L
            val text = withTimeout(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    SmritiLatencyLogger.mark("realGemmaGenerateCallStart")
                    modelLoadAttempted = true
                    engineInitializationAttempted = true
                    conversationCreated = true
                    inferenceAttempted = true
                    var generatedText = ""
                    generationDuration = kotlin.system.measureTimeMillis {
                        generatedText = manualInferenceRunner.generateText(prepared.engineConfig, prompt)
                    }
                    SmritiLatencyLogger.mark("realGemmaGenerateCallEnd")
                    generatedText
                }
            }
            SmritiLatencyLogger.log("realGemmaGenerateCall.${prepared.backendLabel}", generationDuration)
            TextGenerationResult.Success(text)
        } catch (_: TimeoutCancellationException) {
            lease.fail("Timed out after ${timeoutMillis}ms.")
            TextGenerationResult.Failed("Manual LiteRT-LM text inference timed out after ${timeoutMillis}ms.")
        } catch (error: RuntimeException) {
            lease.fail(error.message ?: error::class.java.simpleName)
            TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            lease.fail(error.message ?: error::class.java.simpleName)
            TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: Throwable) {
            lease.fail(error.message ?: error::class.java.simpleName)
            TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        } finally {
            lease.release()
        }
    }

    suspend fun preloadManual(
        allowManualTextInference: Boolean,
        timeoutMillis: Long = DEFAULT_MANUAL_TIMEOUT_MILLIS
    ): RealGemmaPreloadResult {
        val lease = RealGemmaInferenceGate.tryAcquire(
            RealGemmaRequestType.PRELOAD,
            requestDiagnostics(RealGemmaRequestType.PRELOAD)
        ) ?: return RealGemmaPreloadResult.Unavailable(RealGemmaInferenceGate.BUSY_MESSAGE)
        if (!allowManualTextInference) {
            lease.release()
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload skipped: allowManualTextInference=false.")
        }

        val status = modelStatus
        if (status == null) {
            lease.release()
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload unavailable: model status not provided.")
        }

        if (status.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            lease.release()
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload unavailable: model not found.")
        }

        val prepared = engineConfigFactory.prepare(status) as? LiteRtEngineConfigPreparation.Prepared
        if (prepared == null) {
            lease.release()
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload unavailable: EngineConfig not ready.")
        }

        return try {
            withTimeout(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    val duration = kotlin.system.measureTimeMillis {
                        modelLoadAttempted = true
                        engineInitializationAttempted = true
                        conversationCreated = true
                        manualInferenceRunner.preload(prepared.engineConfig)
                    }
                    SmritiLatencyLogger.log("realGemmaEnginePreloadInit.${prepared.backendLabel}", duration)
                }
            }
            RealGemmaPreloadResult.Ready
        } catch (_: TimeoutCancellationException) {
            lease.fail("Preload timed out after ${timeoutMillis}ms.")
            RealGemmaPreloadResult.Failed("Manual LiteRT-LM preload timed out after ${timeoutMillis}ms.")
        } catch (error: RuntimeException) {
            lease.fail(error.message ?: error::class.java.simpleName)
            RealGemmaPreloadResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            lease.fail(error.message ?: error::class.java.simpleName)
            RealGemmaPreloadResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: Throwable) {
            lease.fail(error.message ?: error::class.java.simpleName)
            RealGemmaPreloadResult.Failed(error.message ?: error::class.java.simpleName)
        } finally {
            lease.release()
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
        private var cachedConfigKey: String? = null
        private var failedReason: String? = null

        @Synchronized
        override fun preload(engineConfig: EngineConfig) {
            ensureConversation(engineConfig)
        }

        @Synchronized
        override fun generateText(engineConfig: EngineConfig, prompt: String): String {
            val conversation = ensureConversation(engineConfig)
            return try {
                conversation.sendMessage(prompt).extractText()
            } catch (error: Throwable) {
                failedReason = error.message ?: error::class.java.simpleName
                closeCached()
                throw error
            }
        }

        private fun ensureConversation(engineConfig: EngineConfig): Conversation {
            failedReason?.let { reason ->
                error("LiteRT-LM engine session is marked failed after native call error: $reason. Relaunch the app or explicitly reset the engine before retrying.")
            }
            val configKey = "${engineConfig.modelPath}|${engineConfig.backend.name}"
            val existing = cachedConversation
            if (existing != null && cachedConfigKey == configKey) {
                return existing
            }

            closeCached()
            val engine = Engine(engineConfig)
            engine.initialize()
            val conversation = engine.createConversation()
            cachedEngine = engine
            cachedConversation = conversation
            cachedConfigKey = configKey
            return conversation
        }

        private fun closeCached() {
            runCatching { cachedConversation?.close() }
            runCatching { cachedEngine?.close() }
            cachedConversation = null
            cachedEngine = null
            cachedConfigKey = null
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

        fun engineState(): String {
            val failure = failedReason
            return when {
                failure != null -> "failed: $failure"
                cachedConversation != null -> "conversation_cached"
                cachedEngine != null -> "engine_cached"
                else -> "not_loaded"
            }
        }
    }

    companion object {
        const val DEFAULT_MANUAL_TIMEOUT_MILLIS = 60_000L
    }

    private fun requestDiagnostics(requestType: RealGemmaRequestType): RealGemmaRequestDiagnostics {
        val status = modelStatus
        val prepared = status
            ?.takeIf { it.kind == ModelStatusKind.FOUND_NOT_LOADED }
            ?.let { engineConfigFactory.prepare(it) as? LiteRtEngineConfigPreparation.Prepared }
        return RealGemmaRequestDiagnostics(
            modelExists = status?.kind == ModelStatusKind.FOUND_NOT_LOADED,
            modelSizeBytes = status?.fileSizeBytes,
            sentinelExists = sentinelExists,
            backendMode = prepared?.backendLabel ?: "unavailable",
            engineState = RealManualTextInferenceRunner.engineState(),
            lastEngineFailure = RealGemmaInferenceGate.lastEngineFailure
        ).also {
            SmritiLatencyLogger.mark("realGemmaReadiness requestType=$requestType; ${it.asLogText()}")
        }
    }
}
