package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed class GemmaAudioTranscriptResult {
    data class Success(val transcript: String) : GemmaAudioTranscriptResult()
    data class Unavailable(val reason: String) : GemmaAudioTranscriptResult()
    data class Failed(val reason: String) : GemmaAudioTranscriptResult()
}

class LiteRtGemmaAudioTranscriptClient(
    private val requiredModeStatus: RealGemmaRequiredModeStatus,
    private val modelStatus: ModelStatus,
    private val cacheDirPath: String,
    private val engineConfigFactory: LiteRtEngineConfigFactory = LiteRtEngineConfigFactory(),
    private val runner: AudioTranscriptRunner = RealAudioTranscriptRunner,
    private val sentinelExists: Boolean? = null
) {
    suspend fun transcribe(
        wavAudioBytes: ByteArray,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
    ): GemmaAudioTranscriptResult {
        if (!requiredModeStatus.inferenceEnabled) {
            return GemmaAudioTranscriptResult.Unavailable(UNAVAILABLE_MESSAGE)
        }
        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return GemmaAudioTranscriptResult.Unavailable(UNAVAILABLE_MESSAGE)
        }
        if (wavAudioBytes.isEmpty()) {
            return GemmaAudioTranscriptResult.Failed("Recorded audio was empty.")
        }

        val prepared = engineConfigFactory.prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: return GemmaAudioTranscriptResult.Unavailable(UNAVAILABLE_MESSAGE)
        val audioConfig = prepared.engineConfig.copy(
            audioBackend = Backend.CPU(),
            cacheDir = cacheDirPath
        )
        val lease = RealGemmaInferenceGate.tryAcquire(
            requestType = RealGemmaRequestType.AUDIO_TRANSCRIPT,
            diagnostics = RealGemmaRequestDiagnostics(
                modelExists = true,
                modelSizeBytes = modelStatus.fileSizeBytes,
                sentinelExists = sentinelExists,
                backendMode = "${prepared.backendLabel}; audio=CPU",
                engineState = "audio_new_conversation",
                lastEngineFailure = RealGemmaInferenceGate.lastEngineFailure
            )
        ) ?: return GemmaAudioTranscriptResult.Unavailable(RealGemmaInferenceGate.BUSY_MESSAGE)

        return try {
            var durationMillis = 0L
            val transcript = withTimeout(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    var text = ""
                    durationMillis = kotlin.system.measureTimeMillis {
                        text = runner.transcribe(audioConfig, TRANSCRIPTION_PROMPT, wavAudioBytes)
                    }
                    text.trim()
                }
            }
            SmritiLatencyLogger.log("realGemmaAudioTranscript.CPU", durationMillis)
            if (transcript.isBlank()) {
                GemmaAudioTranscriptResult.Failed("Gemma returned an empty transcript.")
            } else {
                GemmaAudioTranscriptResult.Success(transcript)
            }
        } catch (_: TimeoutCancellationException) {
            lease.fail("Audio transcription timed out.")
            GemmaAudioTranscriptResult.Failed("Gemma audio transcription timed out.")
        } catch (error: RuntimeException) {
            lease.fail(error.message ?: error::class.java.simpleName)
            GemmaAudioTranscriptResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            lease.fail(error.message ?: error::class.java.simpleName)
            GemmaAudioTranscriptResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: Throwable) {
            lease.fail(error.message ?: error::class.java.simpleName)
            GemmaAudioTranscriptResult.Failed(error.message ?: error::class.java.simpleName)
        } finally {
            lease.release()
        }
    }

    fun interface AudioTranscriptRunner {
        fun transcribe(engineConfig: EngineConfig, prompt: String, wavAudioBytes: ByteArray): String
    }

    private object RealAudioTranscriptRunner : AudioTranscriptRunner {
        override fun transcribe(engineConfig: EngineConfig, prompt: String, wavAudioBytes: ByteArray): String {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    val response = conversation.sendMessage(
                        Contents.Companion.of(
                            Content.Text(prompt),
                            Content.AudioBytes(wavAudioBytes)
                        )
                    )
                    return response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString(separator = "\n") { it.text }
                        .trim()
                }
            }
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 60_000L
        const val UNAVAILABLE_MESSAGE =
            "Gemma audio transcription is unavailable. Please type or use offline speech."
        const val TRANSCRIPTION_PROMPT =
            "Transcribe this speech. Output only the transcript."
    }
}
