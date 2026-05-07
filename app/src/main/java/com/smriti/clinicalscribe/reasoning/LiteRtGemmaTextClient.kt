package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class RealGemmaLifecyclePolicy(
    val finalRecordingUi: Boolean = false,
    val freshConversationForVisitNote: Boolean = true,
    val recycleEngineAfterVisitNote: Boolean = false
)

class LiteRtGemmaTextClient(
    private val modelStatus: ModelStatus? = null,
    private val engineConfigFactory: LiteRtEngineConfigFactory = LiteRtEngineConfigFactory(),
    private val apiSurfaceProbe: LiteRtApiSurfaceProbe = LiteRtApiSurfaceProbe(),
    private val manualInferenceRunner: ManualTextInferenceRunner = RealManualTextInferenceRunner,
    private val sentinelExists: Boolean? = null,
    private val lifecyclePolicy: RealGemmaLifecyclePolicy = RealGemmaLifecyclePolicy()
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
        val freshConversation = lifecyclePolicy.freshConversationForVisitNote &&
            requestType == RealGemmaRequestType.VISIT_NOTE
        val recycleEngineAfterRequest = lifecyclePolicy.recycleEngineAfterVisitNote &&
            requestType == RealGemmaRequestType.VISIT_NOTE
        SmritiLatencyLogger.mark(
            "realGemmaLifecycle finalRecordingUi=${lifecyclePolicy.finalRecordingUi}; " +
                "requestType=$requestType; modelExists=true; modelSizeBytes=${status.fileSizeBytes}; " +
                "backendMode=${prepared.backendLabel}; freshConversation=$freshConversation; " +
                "engineRecycledAfterRequest=$recycleEngineAfterRequest; " +
                "engineStateBefore=${manualInferenceRunner.engineState()}; " +
                "requestCountSinceEngineInit=${manualInferenceRunner.requestCountSinceEngineInit()}"
        )

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
                        generatedText = manualInferenceRunner.generateText(
                            engineConfig = prepared.engineConfig,
                            prompt = prompt,
                            requestType = requestType,
                            freshConversation = freshConversation,
                            recycleEngineAfterRequest = recycleEngineAfterRequest
                        )
                    }
                    SmritiLatencyLogger.mark("realGemmaGenerateCallEnd")
                    generatedText
                }
            }
            SmritiLatencyLogger.mark(
                "realGemmaLifecycle requestType=$requestType; " +
                    "engineStateAfter=${manualInferenceRunner.engineState()}; " +
                    "requestCountSinceEngineInit=${manualInferenceRunner.requestCountSinceEngineInit()}"
            )
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
        if (lifecyclePolicy.finalRecordingUi) {
            SmritiLatencyLogger.mark("realGemmaPreloadSkipped finalRecordingUi=true")
            return RealGemmaPreloadResult.Unavailable("Manual LiteRT-LM preload skipped for final recording UI stability.")
        }
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

        fun generateText(
            engineConfig: EngineConfig,
            prompt: String,
            requestType: RealGemmaRequestType,
            freshConversation: Boolean,
            recycleEngineAfterRequest: Boolean
        ): String {
            return generateText(engineConfig, prompt)
        }

        fun preload(engineConfig: EngineConfig) {
            // Default test runners can remain generation-only.
        }

        fun engineState(): String = "external_runner"

        fun requestCountSinceEngineInit(): Int = 0
    }

    private object RealManualTextInferenceRunner : ManualTextInferenceRunner {
        private var cachedEngine: Engine? = null
        private var cachedConversation: Conversation? = null
        private var cachedConfigKey: String? = null
        private var failedReason: String? = null
        private var requestCountSinceEngineInit: Int = 0

        @Synchronized
        override fun preload(engineConfig: EngineConfig) {
            ensureConversation(engineConfig)
        }

        @Synchronized
        override fun generateText(engineConfig: EngineConfig, prompt: String): String {
            return generateText(
                engineConfig = engineConfig,
                prompt = prompt,
                requestType = RealGemmaRequestType.MANUAL_TEST,
                freshConversation = false,
                recycleEngineAfterRequest = false
            )
        }

        @Synchronized
        override fun generateText(
            engineConfig: EngineConfig,
            prompt: String,
            requestType: RealGemmaRequestType,
            freshConversation: Boolean,
            recycleEngineAfterRequest: Boolean
        ): String {
            return try {
                val text = if (freshConversation) {
                    closeCachedConversationOnly()
                    val engine = ensureEngine(engineConfig)
                    requestCountSinceEngineInit += 1
                    SmritiLatencyLogger.mark(
                        "realGemmaLifecycle requestType=$requestType; freshConversation=true; " +
                            "requestCountSinceEngineInit=$requestCountSinceEngineInit"
                    )
                    engine.createConversation().use { conversation ->
                        conversation.sendMessage(prompt).extractText()
                    }
                } else {
                    val conversation = ensureConversation(engineConfig)
                    requestCountSinceEngineInit += 1
                    SmritiLatencyLogger.mark(
                        "realGemmaLifecycle requestType=$requestType; freshConversation=false; " +
                            "requestCountSinceEngineInit=$requestCountSinceEngineInit"
                    )
                    conversation.sendMessage(prompt).extractText()
                }
                if (recycleEngineAfterRequest) {
                    closeCached()
                }
                text
            } catch (error: Throwable) {
                failedReason = error.message ?: error::class.java.simpleName
                closeCached()
                throw error
            }
        }

        private fun ensureEngine(engineConfig: EngineConfig): Engine {
            failedReason?.let { reason ->
                error("LiteRT-LM engine session is marked failed after native call error: $reason. Relaunch the app or explicitly reset the engine before retrying.")
            }
            val configKey = "${engineConfig.modelPath}|${engineConfig.backend.name}"
            val existing = cachedEngine
            if (existing != null && cachedConfigKey == configKey) {
                return existing
            }

            closeCached()
            SmritiLatencyLogger.mark(
                "realGemmaLifecycle engineInitStart; backendMode=${engineConfig.backend.name}; " +
                    "modelPath=${engineConfig.modelPath}"
            )
            return try {
                val engine = Engine(engineConfig)
                engine.initialize()
                cachedEngine = engine
                cachedConfigKey = configKey
                requestCountSinceEngineInit = 0
                SmritiLatencyLogger.mark(
                    "realGemmaLifecycle engineInitSuccess; backendMode=${engineConfig.backend.name}; " +
                        "requestCountSinceEngineInit=$requestCountSinceEngineInit"
                )
                engine
            } catch (error: Throwable) {
                val reason = error.message ?: error::class.java.simpleName
                failedReason = reason
                closeCached()
                SmritiLatencyLogger.mark("realGemmaLifecycle engineInitFailure message=${reason.take(160)}")
                throw error
            }
        }

        private fun ensureConversation(engineConfig: EngineConfig): Conversation {
            val configKey = "${engineConfig.modelPath}|${engineConfig.backend.name}"
            val engine = ensureEngine(engineConfig)
            val existing = cachedConversation
            if (existing != null && cachedConfigKey == configKey) {
                return existing
            }

            closeCachedConversationOnly()
            val conversation = engine.createConversation()
            cachedConversation = conversation
            return conversation
        }

        private fun closeCachedConversationOnly() {
            runCatching { cachedConversation?.close() }
            cachedConversation = null
        }

        private fun closeCached() {
            closeCachedConversationOnly()
            runCatching { cachedEngine?.close() }
            cachedConversation = null
            cachedEngine = null
            cachedConfigKey = null
            requestCountSinceEngineInit = 0
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

        override fun engineState(): String {
            val failure = failedReason
            return when {
                failure != null -> "failed: $failure"
                cachedConversation != null -> "conversation_cached"
                cachedEngine != null -> "engine_cached"
                else -> "not_loaded"
            }
        }

        override fun requestCountSinceEngineInit(): Int = requestCountSinceEngineInit
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
