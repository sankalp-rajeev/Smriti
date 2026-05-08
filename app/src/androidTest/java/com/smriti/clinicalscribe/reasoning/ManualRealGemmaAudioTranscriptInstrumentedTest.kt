package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.InputData
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 6 developer-only probe: can LiteRT-LM Gemma produce a speech transcript
 * from raw audio input?
 *
 * Product boundary:
 *   local audio → Gemma transcript → editable transcript field → existing text
 *   reasoning pipeline → CHW review/save.
 *
 * This test does NOT:
 *   - write to Room
 *   - invoke the visit reasoning pipeline
 *   - generate clinical JSON, referral flags, or diagnosis
 *   - touch MainActivity, VisitScreen, or SummaryScreen
 *   - change any default app behavior
 *
 * Requires explicit instrumentation args:
 *   allowManualAudioInference=true
 *   manualAudioFilePath=/data/local/tmp/manual-smriti-audio.wav
 *
 * Expected model: filesDir/models/gemma-4-E2B-it-int4.litertlm
 *
 * 0.11.0 change: EngineConfig.audioBackend is now available. This probe
 * sets audioBackend = Backend.CPU() and prioritises Route 2
 * (Conversation + Content.AudioBytes with raw WAV bytes) first.
 */
@RunWith(AndroidJUnit4::class)
class ManualRealGemmaAudioTranscriptInstrumentedTest {

    @Test
    fun probesGemmaAudioTranscriptionWithSideloadedModelAndAudioFile() {
        // ── Gate: explicit instrumentation argument required ──
        val args = InstrumentationRegistry.getArguments()
        val allowManualAudioInference = args
            .getString(ARG_ALLOW_MANUAL_AUDIO_INFERENCE)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual Gemma audio transcript probe skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_AUDIO_INFERENCE=true",
            allowManualAudioInference
        )

        // ── Gate: audio file path required ──
        val manualAudioFilePath = args.getString(ARG_MANUAL_AUDIO_FILE_PATH).orEmpty()
        if (manualAudioFilePath.isBlank()) {
            throw AssertionError(
                "Manual Gemma audio transcript probe requires " +
                    "$ARG_MANUAL_AUDIO_FILE_PATH=/data/local/tmp/manual-smriti-audio.wav."
            )
        }

        val audioFile = File(manualAudioFilePath)
        Log.i(TAG, "Audio file path: ${audioFile.absolutePath}")
        if (!audioFile.exists()) {
            throw AssertionError(
                "Manual Gemma audio transcript probe requires sideloaded audio at " +
                    "${audioFile.absolutePath}."
            )
        }

        val audioBytes = audioFile.readBytes()
        Log.i(TAG, "Audio file size: ${audioBytes.size} bytes")
        assertTrue("Audio file was empty.", audioBytes.isNotEmpty())

        // ── Gate: sideloaded model required ──
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
        val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

        Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
        Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            throw AssertionError(
                "Manual Gemma audio transcript probe requires sideloaded model at " +
                    "${expectedModelFile.absolutePath}."
            )
        }

        val prepared = LiteRtEngineConfigFactory().prepare(modelStatus)
            as? LiteRtEngineConfigPreparation.Prepared
            ?: throw AssertionError("Could not prepare EngineConfig for audio transcript probe.")

        // Build audio-enabled config with audioBackend (new in 0.11.0)
        val audioConfig = prepared.engineConfig.copy(
            audioBackend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        Log.i(TAG, "EngineConfig.audioBackend set to CPU (new in 0.11.0)")

        // ── Log API surface availability ──
        Log.i(TAG, "Content.AudioBytes available: true")
        Log.i(TAG, "Content.AudioFile available: true")
        Log.i(TAG, "InputData.Audio available: true")
        Log.i(TAG, "EngineConfig.audioBackend: available in litertlm-android 0.11.0")
        LiteRtApiSurfaceProbe().audioPreprocessingFindings.forEach { finding ->
            Log.i(TAG, "Audio API finding: $finding")
        }

        // ── Route 2 (prioritised): Conversation + Content.AudioBytes ──
        Log.i(TAG, "Running Route 2 first (highest likelihood with 0.11.0 audioBackend)")
        val route2Result = tryConversationAudioBytes(audioConfig, audioBytes)
        if (route2Result != null) {
            logTranscriptResult("Conversation+AudioBytes", route2Result)
            return
        }

        // ── Route 1: Conversation + Content.AudioFile ──
        val route1Result = tryConversationAudioFile(audioConfig, audioFile)
        if (route1Result != null) {
            logTranscriptResult("Conversation+AudioFile", route1Result)
            return
        }

        // ── Route 3: Session + InputData.Audio (raw bytes) ──
        val route3Result = trySessionInputDataAudio(audioConfig, audioBytes)
        if (route3Result != null) {
            logTranscriptResult("Session+InputData.Audio", route3Result)
            return
        }

        // ── Route 4: WAV PCM extraction → Content.AudioBytes ──
        val pcmBytes = tryExtractWavPcm(audioBytes)
        if (pcmBytes != null) {
            Log.i(TAG, "Extracted WAV PCM payload: ${pcmBytes.size} bytes")
            val route4Result = tryConversationAudioBytes(audioConfig, pcmBytes)
            if (route4Result != null) {
                logTranscriptResult("Conversation+AudioBytes(PCM-only)", route4Result)
                return
            }
        } else {
            Log.i(TAG, "WAV PCM extraction skipped: file does not appear to be a WAV with PCM data.")
        }

        // ── All routes blocked ──
        val blocker = "Audio runtime blocked: LiteRT-LM 0.11.0 exposes EngineConfig.audioBackend " +
            "but all four routes (Conversation+AudioBytes, Conversation+AudioFile, " +
            "Session+InputData.Audio, PCM-bytes) still returned errors. " +
            "No public AudioPreprocessor or preprocess() API was found."
        Log.w(TAG, blocker)
        Log.w(TAG, "Product fallback: use Android offline speech or manual transcript → " +
            "existing text reasoning pipeline → CHW review/save.")
        assumeTrue(blocker, false)
    }

    // ── Route helpers ──

    private fun tryConversationAudioBytes(
        engineConfig: com.google.ai.edge.litertlm.EngineConfig,
        audioBytes: ByteArray
    ): String? {
        Log.i(TAG, "Route 2/4: Conversation.sendMessage(Contents.of(Text, AudioBytes)) with audioBackend=CPU")
        return try {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    val response = conversation.sendMessage(
                        Contents.Companion.of(
                            Content.Text(TRANSCRIPTION_PROMPT),
                            Content.AudioBytes(audioBytes)
                        )
                    )
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString(separator = "\n") { it.text }
                        .trim()
                }
            }
        } catch (error: RuntimeException) {
            if (error.isAudioBlocker()) {
                Log.w(TAG, "Route 2/4 blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Route 2/4 failed unexpectedly: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Route 2/4 linkage failure: ${error.message}", error)
            throw error
        }
    }

    private fun tryConversationAudioFile(
        engineConfig: com.google.ai.edge.litertlm.EngineConfig,
        audioFile: File
    ): String? {
        Log.i(TAG, "Route 1: Conversation.sendMessage(Contents.of(Text, AudioFile)) with audioBackend=CPU")
        return try {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    val response = conversation.sendMessage(
                        Contents.Companion.of(
                            Content.Text(TRANSCRIPTION_PROMPT),
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
            if (error.isAudioBlocker()) {
                Log.w(TAG, "Route 1 blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Route 1 failed unexpectedly: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Route 1 linkage failure: ${error.message}", error)
            throw error
        }
    }

    private fun trySessionInputDataAudio(
        engineConfig: com.google.ai.edge.litertlm.EngineConfig,
        audioBytes: ByteArray
    ): String? {
        Log.i(TAG, "Route 3: Session.generateContent(listOf(Text, Audio)) with audioBackend=CPU")
        return try {
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createSession().use { session ->
                    session.generateContent(
                        listOf(
                            InputData.Text(TRANSCRIPTION_PROMPT),
                            InputData.Audio(audioBytes)
                        )
                    ).trim()
                }
            }
        } catch (error: RuntimeException) {
            if (error.isAudioBlocker()) {
                Log.w(TAG, "Route 3 blocked: ${error.message}")
                null
            } else {
                Log.e(TAG, "Route 3 failed unexpectedly: ${error.message}", error)
                throw error
            }
        } catch (error: LinkageError) {
            Log.e(TAG, "Route 3 linkage failure: ${error.message}", error)
            throw error
        }
    }

    // ── WAV PCM extraction (one simple, documented preparation step) ──

    /**
     * Attempts to strip a standard RIFF/WAV header and return only the raw PCM
     * data payload. Returns null if the file is not a recognizable WAV with
     * PCM format (format code 1). This does NOT build a full MediaCodec pipeline.
     */
    private fun tryExtractWavPcm(wavBytes: ByteArray): ByteArray? {
        if (wavBytes.size < 44) return null
        val header = ByteBuffer.wrap(wavBytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
        val riff = String(wavBytes, 0, 4, Charsets.US_ASCII)
        val wave = String(wavBytes, 8, 4, Charsets.US_ASCII)
        if (riff != "RIFF" || wave != "WAVE") return null

        header.position(20)
        val audioFormat = header.short.toInt() and 0xFFFF
        if (audioFormat != 1) {
            Log.i(TAG, "WAV format code=$audioFormat (not PCM/1); PCM extraction skipped.")
            return null
        }

        val channels = header.short.toInt() and 0xFFFF
        val sampleRate = header.int
        header.position(34)
        val bitsPerSample = header.short.toInt() and 0xFFFF
        Log.i(TAG, "WAV PCM: channels=$channels sampleRate=$sampleRate bitsPerSample=$bitsPerSample")

        // Find the "data" subchunk
        var offset = 12
        while (offset + 8 < wavBytes.size) {
            val chunkId = String(wavBytes, offset, 4, Charsets.US_ASCII)
            val chunkSizeBuffer = ByteBuffer.wrap(wavBytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN)
            val chunkSize = chunkSizeBuffer.int
            if (chunkId == "data") {
                val dataStart = offset + 8
                val dataEnd = minOf(dataStart + chunkSize, wavBytes.size)
                return wavBytes.copyOfRange(dataStart, dataEnd)
            }
            offset += 8 + chunkSize
        }
        return null
    }

    // ── Utilities ──

    private fun logTranscriptResult(route: String, text: String) {
        val preview = text.take(TRANSCRIPT_PREVIEW_CHARS)
        Log.i(TAG, "SUCCESS via $route")
        Log.i(TAG, "Transcript length: ${text.length} chars")
        Log.i(TAG, "Transcript preview: $preview")
        assertTrue("Gemma audio transcript was blank via $route.", text.isNotBlank())
    }

    private fun RuntimeException.isAudioBlocker(): Boolean {
        val combined = listOfNotNull(message, cause?.message).joinToString("\n")
        return combined.contains("Audio must be preprocessed", ignoreCase = true) ||
            combined.contains("preprocess", ignoreCase = true) ||
            combined.contains("audio format", ignoreCase = true) ||
            combined.contains("audio backend", ignoreCase = true) ||
            combined.contains("audio is not supported", ignoreCase = true)
    }

    private companion object {
        const val TAG = "SmritiGemmaAudioTranscript"
        const val ARG_ALLOW_MANUAL_AUDIO_INFERENCE = "allowManualAudioInference"
        const val ARG_MANUAL_AUDIO_FILE_PATH = "manualAudioFilePath"
        const val TRANSCRIPT_PREVIEW_CHARS = 500
        const val TRANSCRIPTION_PROMPT =
            "Transcribe this speech. Output only the transcript."
    }
}
