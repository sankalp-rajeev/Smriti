package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.InputData
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualLiteRtAudioCapabilityInstrumentedTest {
    @Test
    fun probesLiteRtAudioApiSurfaceAndOptionalManualAudioInference() {
        val args = InstrumentationRegistry.getArguments()
        val allowManualAudioProbe = args
            .getString(ARG_ALLOW_MANUAL_AUDIO_PROBE)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual LiteRT audio capability probe skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_AUDIO_PROBE=true",
            allowManualAudioProbe
        )

        val audioBytesContent = Content.AudioBytes(byteArrayOf(0, 1, 2, 3))
        val audioFileContent = Content.AudioFile("/data/local/tmp/manual-smriti-audio.wav")
        val audioInputData = InputData.Audio(byteArrayOf(0, 1, 2, 3))
        val textInputData = InputData.Text("Transcribe or summarize this audio for CHW review. This is not a diagnosis.")

        Log.i(TAG, "Content.AudioBytes API available: bytes=${audioBytesContent.bytes.size}")
        Log.i(TAG, "Content.AudioFile API available: path=${audioFileContent.absolutePath}")
        Log.i(TAG, "InputData.Audio API available: bytes=${audioInputData.bytes.size}")
        Log.i(TAG, "InputData.Text API available: textLength=${textInputData.text.length}")
        Log.i(TAG, "Audio API finding: LiteRT-LM Android 0.11.0 exposes audio content/input classes.")
        LiteRtApiSurfaceProbe().audioPreprocessingFindings.forEach { finding ->
            Log.i(TAG, "Audio preprocessing API finding: $finding")
        }
        Log.i(TAG, "Audio runtime status: not wired into Smriti UI; use ManualLiteRtAudioInferenceInstrumentedTest for explicit runtime probing.")
    }

    private companion object {
        const val TAG = "SmritiLiteRtAudioProbe"
        const val ARG_ALLOW_MANUAL_AUDIO_PROBE = "allowManualAudioProbe"
    }
}
