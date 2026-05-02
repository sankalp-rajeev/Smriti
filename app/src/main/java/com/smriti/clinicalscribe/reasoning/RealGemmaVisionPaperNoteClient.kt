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

sealed class PaperNoteVisionGenerationResult {
    data class Success(val rawText: String) : PaperNoteVisionGenerationResult()
    data class Unavailable(val reason: String) : PaperNoteVisionGenerationResult()
    data class Failed(val reason: String) : PaperNoteVisionGenerationResult()
}

class RealGemmaVisionPaperNoteClient(
    private val modelStatus: ModelStatus,
    private val cacheDirPath: String,
    private val engineConfigFactory: LiteRtEngineConfigFactory = LiteRtEngineConfigFactory(),
    private val runner: VisionInferenceRunner = RealVisionInferenceRunner
) {
    suspend fun extractPaperNote(
        imageBytes: ByteArray,
        prompt: String = PAPER_NOTE_EXTRACTION_PROMPT,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
    ): PaperNoteVisionGenerationResult {
        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            return PaperNoteVisionGenerationResult.Unavailable(
                "Local Gemma vision setup needed before paper-note extraction."
            )
        }
        if (imageBytes.isEmpty()) {
            return PaperNoteVisionGenerationResult.Failed("Selected image was empty.")
        }

        val prepared = engineConfigFactory.prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: return PaperNoteVisionGenerationResult.Unavailable("Local Gemma vision EngineConfig is not ready.")
        val visionConfig = prepared.engineConfig.copy(
            visionBackend = Backend.CPU(),
            maxNumImages = 1,
            cacheDir = cacheDirPath
        )

        return try {
            val raw = withTimeout(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    runner.generate(visionConfig, prompt, imageBytes)
                }
            }
            if (raw.isBlank()) {
                PaperNoteVisionGenerationResult.Failed("Local Gemma vision returned no text.")
            } else {
                PaperNoteVisionGenerationResult.Success(raw)
            }
        } catch (_: TimeoutCancellationException) {
            PaperNoteVisionGenerationResult.Failed("Local Gemma vision timed out.")
        } catch (error: RuntimeException) {
            PaperNoteVisionGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            PaperNoteVisionGenerationResult.Failed(error.message ?: error::class.java.simpleName)
        }
    }

    fun interface VisionInferenceRunner {
        fun generate(engineConfig: EngineConfig, prompt: String, imageBytes: ByteArray): String
    }

    private object RealVisionInferenceRunner : VisionInferenceRunner {
        override fun generate(engineConfig: EngineConfig, prompt: String, imageBytes: ByteArray): String {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    val response = conversation.sendMessage(
                        Contents.Companion.of(
                            Content.Text(prompt),
                            Content.ImageBytes(imageBytes)
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
        val PAPER_NOTE_EXTRACTION_PROMPT = """
            Extract only what is written in the paper note image. Do not diagnose. Do not infer referral need. Do not add recommendations not written in the note. Return JSON only.

            This is data entry support only for a health worker review screen.
            Do not analyze photos of people, wounds, rashes, ultrasound images, medicine strips, or growth charts.
            Do not generate referral advice from this image alone.

            Return exactly this JSON schema:
            {
              "patientName": "string",
              "visitDate": "string",
              "bloodPressure": "string",
              "symptoms": ["string"],
              "followUpPlan": "string",
              "confidence": "HIGH|MEDIUM|LOW",
              "needsReview": true,
              "safetyNote": "Extracted from image. Health worker must review before saving."
            }

            If text is unclear, set confidence to LOW and leave unclear fields blank.
        """.trimIndent()
    }
}
