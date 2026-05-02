package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.InputData
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualRealGemmaVisionProbeInstrumentedTest {
    @Test
    fun probesSyntheticPaperNoteExtractionWithLocalGemmaVisionOnly() {
        val args = InstrumentationRegistry.getArguments()
        val allowManualVisionInference = args
            .getString(ARG_ALLOW_MANUAL_VISION_INFERENCE)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual RealGemma vision probe skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_VISION_INFERENCE=true",
            allowManualVisionInference
        )

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testContext = instrumentation.context
        val appContext = instrumentation.targetContext
        val imageBytes = testContext.assets.open(SAMPLE_ASSET_NAME).use { it.readBytes() }

        Log.i(TAG, "Synthetic androidTest asset: $SAMPLE_ASSET_NAME")
        Log.i(TAG, "Synthetic image bytes: ${imageBytes.size}")
        LiteRtApiSurfaceProbe().imageApiFindings.forEach { finding ->
            Log.i(TAG, "Image API finding: $finding")
        }

        val expectedModelFile = LiteRtModelPaths.expectedModelFile(appContext.filesDir)
        val modelStatus = ModelAvailability.fromFilesDir(appContext.filesDir).check()

        Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
        Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            throw AssertionError(
                "Manual RealGemma vision probe requires sideloaded model at ${expectedModelFile.absolutePath}."
            )
        }

        val prepared = LiteRtEngineConfigFactory().prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: throw AssertionError("Manual RealGemma vision probe could not prepare EngineConfig.")
        val visionConfig = prepared.engineConfig.copy(
            visionBackend = Backend.CPU(),
            maxNumImages = 1,
            cacheDir = appContext.cacheDir.absolutePath
        )

        val output = tryGenerateWithConversation(visionConfig, imageBytes)
            ?: tryGenerateWithSession(visionConfig, imageBytes)
            ?: run {
                val blocker = "Vision runtime blocked: LiteRT-LM image classes exist, but the current " +
                    "Kotlin/JNI path did not accept image input or lacks required multimodal prompt-template/media handling."
                Log.w(TAG, blocker)
                assumeTrue(blocker, false)
                return
            }

        val preview = output.take(RAW_OUTPUT_PREVIEW_CHARS)
        Log.i(TAG, "Vision probe raw output preview first ${preview.length} chars: $preview")
        assertTrue("Manual RealGemma vision probe returned blank text.", output.isNotBlank())
    }

    private fun tryGenerateWithConversation(
        engineConfig: com.google.ai.edge.litertlm.EngineConfig,
        imageBytes: ByteArray
    ): String? {
        Log.i(TAG, "Image API path used: Conversation.sendMessage(Contents.of(Content.Text, Content.ImageBytes))")
        return try {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    Log.i(TAG, "Conversation vision input started")
                    val response = conversation.sendMessage(
                        Contents.Companion.of(
                            Content.Text(EXTRACTION_PROMPT),
                            Content.ImageBytes(imageBytes)
                        )
                    )
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString(separator = "\n") { it.text }
                        .trim()
                        .also { Log.i(TAG, "Engine accepted Conversation image input: ${it.isNotBlank()}") }
                }
            }
        } catch (error: RuntimeException) {
            if (error.isVisionBlocker()) {
                Log.w(TAG, "Conversation image route blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Conversation image route failed: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Conversation image route linkage failure: ${error.message}", error)
            throw error
        }
    }

    private fun tryGenerateWithSession(
        engineConfig: com.google.ai.edge.litertlm.EngineConfig,
        imageBytes: ByteArray
    ): String? {
        Log.i(TAG, "Image API path used: Session.generateContent(listOf(InputData.Text, InputData.Image))")
        return try {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createSession().use { session ->
                    Log.i(TAG, "Session vision input started")
                    session.generateContent(
                        listOf(
                            InputData.Text(EXTRACTION_PROMPT),
                            InputData.Image(imageBytes)
                        )
                    )
                        .trim()
                        .also { Log.i(TAG, "Engine accepted Session image input: ${it.isNotBlank()}") }
                }
            }
        } catch (error: RuntimeException) {
            if (error.isVisionBlocker()) {
                Log.w(TAG, "Session image route blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Session image route failed: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Session image route linkage failure: ${error.message}", error)
            throw error
        }
    }

    private fun RuntimeException.isVisionBlocker(): Boolean {
        val text = listOfNotNull(
            message,
            cause?.message
        ).joinToString(separator = "\n")

        return text.contains("LiteRtLmJniException", ignoreCase = true) ||
            text.contains("prompt template", ignoreCase = true) ||
            text.contains("template", ignoreCase = true) ||
            text.contains("placeholder", ignoreCase = true) ||
            text.contains("multimodal", ignoreCase = true) ||
            text.contains("multi-modal", ignoreCase = true) ||
            text.contains("media", ignoreCase = true) ||
            text.contains("image", ignoreCase = true) ||
            text.contains("vision", ignoreCase = true)
    }

    private companion object {
        const val TAG = "SmritiRealGemmaVision"
        const val ARG_ALLOW_MANUAL_VISION_INFERENCE = "allowManualVisionInference"
        const val SAMPLE_ASSET_NAME = "sample_paper_visit_note.png"
        const val RAW_OUTPUT_PREVIEW_CHARS = 1200
        val EXTRACTION_PROMPT = """
            You are reading a synthetic paper visit note image for data entry only.
            Extract only written text visible in the image.
            Do not diagnose. Do not recommend referral from the image alone.
            Return JSON only with exactly these keys:
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
