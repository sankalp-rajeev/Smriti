package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtGemmaAudioTranscriptClientTest {
    @After
    fun tearDown() {
        RealGemmaInferenceGate.resetForTests()
    }

    @Test
    fun gatedAudioTranscriptionUsesAudioBytesAndReturnsTranscriptOnly() = runBlocking {
        val runner = RecordingAudioRunner("Meena reported severe headache.")
        val client = client(runner = runner)

        val result = client.transcribe(byteArrayOf(1, 2, 3))

        assertTrue(result is GemmaAudioTranscriptResult.Success)
        assertEquals("Meena reported severe headache.", (result as GemmaAudioTranscriptResult.Success).transcript)
        assertEquals(LiteRtGemmaAudioTranscriptClient.TRANSCRIPTION_PROMPT, runner.prompt)
        assertEquals(byteArrayOf(1, 2, 3).toList(), runner.audioBytes?.toList())
        assertEquals("CPU", runner.audioBackendName)
    }

    @Test
    fun unavailableWhenSubmissionReadinessIsMissing() = runBlocking {
        val missingGate = RealGemmaRequiredMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = false,
            modelStatus = foundModel()
        )
        val runner = RecordingAudioRunner("should not run")
        val client = client(status = missingGate, runner = runner)

        val result = client.transcribe(byteArrayOf(1))

        assertEquals(
            LiteRtGemmaAudioTranscriptClient.UNAVAILABLE_MESSAGE,
            (result as GemmaAudioTranscriptResult.Unavailable).reason
        )
        assertFalse(runner.called)
    }

    @Test
    fun singleFlightGatePreventsOverlapWithOtherRealGemmaRequests() = runBlocking {
        val lease = RealGemmaInferenceGate.tryAcquire(
            requestType = RealGemmaRequestType.VISIT_NOTE,
            diagnostics = RealGemmaRequestDiagnostics(
                modelExists = true,
                modelSizeBytes = 123L,
                sentinelExists = true,
                backendMode = "CPU",
                engineState = "test",
                lastEngineFailure = null
            )
        ) ?: error("Expected test lease")
        val runner = RecordingAudioRunner("should not run")
        val client = client(runner = runner)

        val result = client.transcribe(byteArrayOf(1))

        lease.release()
        assertEquals(
            RealGemmaInferenceGate.BUSY_MESSAGE,
            (result as GemmaAudioTranscriptResult.Unavailable).reason
        )
        assertFalse(runner.called)
    }

    @Test
    fun blankTranscriptFailsInsteadOfCreatingOutput() = runBlocking {
        val client = client(runner = RecordingAudioRunner("   "))

        val result = client.transcribe(byteArrayOf(1))

        assertEquals(
            "Gemma returned an empty transcript.",
            (result as GemmaAudioTranscriptResult.Failed).reason
        )
    }

    private fun client(
        status: RealGemmaRequiredModeStatus = readyStatus(),
        runner: RecordingAudioRunner
    ): LiteRtGemmaAudioTranscriptClient {
        return LiteRtGemmaAudioTranscriptClient(
            requiredModeStatus = status,
            modelStatus = status.modelStatus,
            cacheDirPath = "cache",
            runner = runner,
            sentinelExists = status.localGateEnabled
        )
    }

    private class RecordingAudioRunner(
        private val response: String
    ) : LiteRtGemmaAudioTranscriptClient.AudioTranscriptRunner {
        var called = false
        var prompt: String? = null
        var audioBytes: ByteArray? = null
        var audioBackendName: String? = null

        override fun transcribe(engineConfig: EngineConfig, prompt: String, wavAudioBytes: ByteArray): String {
            called = true
            this.prompt = prompt
            this.audioBytes = wavAudioBytes
            audioBackendName = engineConfig.audioBackend?.name
            return response
        }
    }

    private fun readyStatus(): RealGemmaRequiredModeStatus {
        return RealGemmaRequiredMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = true,
            modelStatus = foundModel()
        )
    }

    private fun foundModel(): ModelStatus {
        return ModelStatus(
            kind = ModelStatusKind.FOUND_NOT_LOADED,
            expectedPath = "/tmp/gemma-4-E2B-it-int4.litertlm",
            fileSizeBytes = 123L
        )
    }
}
