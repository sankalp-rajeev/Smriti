package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualLiteRtTextInferenceInstrumentedTest {
    @Test
    fun runsOneManualTextInferenceWithSideloadedModel() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val allowManualTextInference = InstrumentationRegistry.getArguments()
            .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual LiteRT text inference skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_TEXT_INFERENCE=true",
            allowManualTextInference
        )

        val context = instrumentation.targetContext
        val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
        val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

        Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
        Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

        assumeTrue(
            "Manual LiteRT model missing at ${expectedModelFile.absolutePath}. " +
                "Sideload ${LiteRtModelPaths.GEMMA_E2B_MODEL_FILE_NAME} before running this test.",
            modelStatus.kind == ModelStatusKind.FOUND_NOT_LOADED
        )

        val client = LiteRtGemmaTextClient(modelStatus = modelStatus)
        val result = client.generateTextManual(
            prompt = PROMPT,
            allowManualTextInference = allowManualTextInference,
            timeoutMillis = TIMEOUT_MILLIS
        )

        when (result) {
            is TextGenerationResult.Success -> {
                Log.i(TAG, "Manual LiteRT text inference result: ${result.text}")
                assertTrue(
                    "Manual LiteRT text inference returned blank text.",
                    result.text.isNotBlank()
                )
            }
            is TextGenerationResult.Unavailable -> {
                Log.e(TAG, "Manual LiteRT text inference unavailable: ${result.status}")
                throw AssertionError(result.status)
            }
            is TextGenerationResult.Failed -> {
                Log.e(TAG, "Manual LiteRT text inference failed: ${result.error}")
                throw AssertionError(result.error)
            }
        }
    }

    private companion object {
        const val TAG = "SmritiLiteRtManualTest"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val PROMPT = "Reply with exactly: SMRITI_LITERT_OK"
        const val TIMEOUT_MILLIS = 120_000L
    }
}
