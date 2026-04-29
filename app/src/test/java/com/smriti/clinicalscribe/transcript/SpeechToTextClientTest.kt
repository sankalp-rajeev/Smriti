package com.smriti.clinicalscribe.transcript

import android.speech.SpeechRecognizer
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechToTextClientTest {
    @Test
    fun simulatedTranscriptReturnsSuccess() = runBlocking {
        val result = SimulatedTranscriptClient("Manual Hindi English observation")
            .transcribeAudioFile("local/audio.m4a")

        assertTrue(result is TranscriptResult.Success)
        val success = result as TranscriptResult.Success
        assertEquals("Manual Hindi English observation", success.transcript)
        assertEquals(TranscriptSourceKind.SIMULATED, success.metadata.source)
        assertTrue(success.metadata.isOffline)
    }

    @Test
    fun simulatedClientCanReturnUnavailablePath() = runBlocking {
        val result = SimulatedTranscriptClient(
            resultOverride = TranscriptResult.Unavailable("Offline ASR pack not installed.")
        ).transcribeAudioFile("local/audio.m4a")

        assertTrue(result is TranscriptResult.Unavailable)
        assertTrue((result as TranscriptResult.Unavailable).reason.contains("Offline ASR pack"))
    }

    @Test
    fun simulatedClientCanReturnErrorPath() = runBlocking {
        val result = SimulatedTranscriptClient("").transcribeAudioFile("local/audio.m4a")

        assertTrue(result is TranscriptResult.Error)
        assertTrue((result as TranscriptResult.Error).reason.contains("blank"))
    }

    @Test
    fun transcriptLayerHasNoNetworkOrCloudAssumptions() {
        val sourceText = transcriptSourceFiles().joinToString(separator = "\n") { it.readText() }.lowercase()

        assertFalse(sourceText.contains("http://"))
        assertFalse(sourceText.contains("https://"))
        assertFalse(sourceText.contains("openai"))
        assertFalse(sourceText.contains("gemini"))
        assertFalse(sourceText.contains("firebase"))
        assertFalse(sourceText.contains("cloud asr"))
    }

    @Test
    fun androidOfflineRecognizerSkeletonRequestsOfflineRecognition() {
        val sourceText = transcriptSourceFiles()
            .first { it.name == "AndroidOfflineSpeechRecognizerClient.kt" }
            .readText()

        assertTrue(sourceText.contains("RecognizerIntent.EXTRA_PREFER_OFFLINE"))
        assertTrue(sourceText.contains("putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)"))
        assertTrue(sourceText.contains("transcribeLiveSpeech"))
        assertFalse(sourceText.contains("ACTION_WEB_SEARCH"))
        assertFalse(sourceText.contains("upload"))
    }

    @Test
    fun storedAudioFileTranscriptionRemainsManualFallbackOnly() {
        val sourceText = transcriptSourceFiles()
            .first { it.name == "AndroidOfflineSpeechRecognizerClient.kt" }
            .readText()

        assertTrue(sourceText.contains("stored audio-file transcription"))
        assertTrue(sourceText.contains("requires a local ASR engine"))
        assertTrue(sourceText.contains("Please provide or edit the manual transcript"))
    }

    @Test
    fun androidNetworkAndServerRecognitionErrorsAreUnavailable() {
        val network = AndroidOfflineSpeechRecognizerClient.resultForRecognitionError(SpeechRecognizer.ERROR_NETWORK)
        val timeout = AndroidOfflineSpeechRecognizerClient.resultForRecognitionError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)
        val server = AndroidOfflineSpeechRecognizerClient.resultForRecognitionError(SpeechRecognizer.ERROR_SERVER)

        assertTrue(network is TranscriptResult.Unavailable)
        assertTrue(timeout is TranscriptResult.Unavailable)
        assertTrue(server is TranscriptResult.Unavailable)
        assertTrue((network as TranscriptResult.Unavailable).reason.contains("manual transcript"))
    }

    @Test
    fun androidNoMatchAndSpeechTimeoutAskForManualTranscript() {
        val noMatch = AndroidOfflineSpeechRecognizerClient.resultForRecognitionError(SpeechRecognizer.ERROR_NO_MATCH)
        val timeout = AndroidOfflineSpeechRecognizerClient.resultForRecognitionError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

        assertTrue(noMatch is TranscriptResult.Unavailable)
        assertTrue(timeout is TranscriptResult.Unavailable)
        assertTrue((noMatch as TranscriptResult.Unavailable).reason.contains("manual transcript"))
    }

    private fun transcriptSourceFiles(): List<File> {
        val moduleDir = File("src/main/java/com/smriti/clinicalscribe/transcript")
        val rootDir = File("app/src/main/java/com/smriti/clinicalscribe/transcript")
        val dir = if (moduleDir.exists()) moduleDir else rootDir
        return dir.listFiles { file -> file.extension == "kt" }.orEmpty().toList()
    }
}
