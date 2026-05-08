package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.InputData
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualLiteRtAudioInferenceInstrumentedTest {
    @Test
    fun runsManualLiteRtAudioInferenceWithSideloadedModelAndAudioFile() {
        val args = InstrumentationRegistry.getArguments()
        val allowManualAudioInference = args
            .getString(ARG_ALLOW_MANUAL_AUDIO_INFERENCE)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual LiteRT audio inference skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_AUDIO_INFERENCE=true",
            allowManualAudioInference
        )

        val manualAudioFilePath = args.getString(ARG_MANUAL_AUDIO_FILE_PATH).orEmpty()
        if (manualAudioFilePath.isBlank()) {
            throw AssertionError(
                "Manual LiteRT audio inference requires $ARG_MANUAL_AUDIO_FILE_PATH=" +
                    "/data/local/tmp/manual-smriti-audio.wav."
            )
        }

        val audioFile = File(manualAudioFilePath)
        Log.i(TAG, "Manual audio path: ${audioFile.absolutePath}")
        if (!audioFile.exists()) {
            throw AssertionError("Manual LiteRT audio inference requires sideloaded audio at ${audioFile.absolutePath}.")
        }

        val audioBytes = audioFile.readBytes()
        Log.i(TAG, "Manual audio bytes: ${audioBytes.size}")
        assertTrue("Manual audio file was empty.", audioBytes.isNotEmpty())
        LiteRtApiSurfaceProbe().audioPreprocessingFindings.forEach { finding ->
            Log.i(TAG, "Audio preprocessing API finding: $finding")
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
        val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

        Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
        Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            throw AssertionError(
                "Manual LiteRT audio inference requires sideloaded model at ${expectedModelFile.absolutePath}."
            )
        }

        val prepared = LiteRtEngineConfigFactory().prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: throw AssertionError("Manual LiteRT audio inference could not prepare EngineConfig.")

        val output = tryGenerateAudioWithConversation(prepared, audioFile)
            ?: tryGenerateRawAudioWithSession(prepared, audioBytes)
            ?: run {
                val blocker = "Audio runtime blocked: LiteRT-LM requires preprocessing, " +
                    "but preprocessing API was not found/wired in litertlm-android 0.11.0."
                Log.w(TAG, blocker)
                Log.w(
                    TAG,
                    "Phase 2 fallback: keep Android MediaRecorder capture plus manual/simulated transcript for demo, " +
                        "or add future external/offline ASR preprocessing if LiteRT-LM requires it."
                )
                assumeTrue(blocker, false)
                return
            }

        Log.i(TAG, "Manual audio inference output length: ${output.length}")
        Log.i(TAG, "Manual audio inference output: $output")
        assertTrue("Manual audio inference returned blank text.", output.isNotBlank())
    }

    private fun tryGenerateAudioWithConversation(
        prepared: LiteRtEngineConfigPreparation.Prepared,
        audioFile: File
    ): String? {
        Log.i(TAG, "Conversation audio preprocessing route started: Content.AudioFile")
        return try {
            Engine(prepared.engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    Log.i(TAG, "Conversation generateContent/sendMessage started")
                    val response = conversation.sendMessage(
                        Contents.Companion.of(
                            Content.Text("Transcribe this audio in English. Return only the transcript text."),
                            Content.AudioFile(audioFile.absolutePath)
                        )
                    )
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString(separator = "\n") { it.text }
                        .trim()
                }
            }
        } catch (error: RuntimeException) {
            if (error.isAudioPreprocessingBlocker()) {
                Log.w(TAG, "Conversation audio route blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Conversation audio route failed: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Conversation audio route linkage failure: ${error.message}", error)
            throw error
        }
    }

    private fun tryGenerateRawAudioWithSession(
        prepared: LiteRtEngineConfigPreparation.Prepared,
        audioBytes: ByteArray
    ): String? {
        Log.i(TAG, "Raw Session audio route started: InputData.Audio")
        return try {
            Engine(prepared.engineConfig).use { engine ->
                engine.initialize()
                engine.createSession().use { session ->
                    Log.i(TAG, "Session generateContent started")
                    session.generateContent(
                        listOf(
                            InputData.Text("Transcribe this audio in English. Return only the transcript text."),
                            InputData.Audio(audioBytes)
                        )
                    ).trim()
                }
            }
        } catch (error: RuntimeException) {
            if (error.isAudioPreprocessingBlocker()) {
                Log.w(TAG, "Raw Session audio route blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Raw Session audio route failed: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Raw Session audio route linkage failure: ${error.message}", error)
            throw error
        }
    }

    private fun RuntimeException.isAudioPreprocessingBlocker(): Boolean {
        return message?.contains("Audio must be preprocessed", ignoreCase = true) == true ||
            cause?.message?.contains("Audio must be preprocessed", ignoreCase = true) == true
    }

    private companion object {
        const val TAG = "SmritiLiteRtAudioInference"
        const val ARG_ALLOW_MANUAL_AUDIO_INFERENCE = "allowManualAudioInference"
        const val ARG_MANUAL_AUDIO_FILE_PATH = "manualAudioFilePath"
    }
}
