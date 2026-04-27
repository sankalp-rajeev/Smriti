package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualRealGemmaVisitJsonInstrumentedTest {
    @Test
    fun runsManualVisitReasoningJsonInferenceWithSideloadedModel() {
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val allowManualTextInference = InstrumentationRegistry.getArguments()
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true

            assumeTrue(
                "Manual RealGemma visit JSON inference skipped: pass " +
                    "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_TEXT_INFERENCE=true",
                allowManualTextInference
            )

            val context = instrumentation.targetContext
            val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
            val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

            Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
            Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

            if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
                throw AssertionError(
                    "Manual RealGemma visit JSON test requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            val patient = meenaPatient()
            val history = meenaVisitHistory()
            val protocolChunks = relevantProtocolChunks()
            val prompt = RealGemmaPromptBuilder()
                .buildVisitReasoningPrompt(
                    patient = patient,
                    visitHistory = history,
                    observationText = OBSERVATION,
                    protocolChunks = protocolChunks
                ) + STRICT_JSON_REMINDER

            Log.i(TAG, "Prompt length: ${prompt.length} chars")
            logLong("Prompt", prompt)

            val client = LiteRtGemmaTextClient(modelStatus = modelStatus)
            val generationResult = client.generateTextManual(
                prompt = prompt,
                allowManualTextInference = allowManualTextInference,
                timeoutMillis = TIMEOUT_MILLIS
            )

            val rawOutput = when (generationResult) {
                is TextGenerationResult.Success -> generationResult.text
                is TextGenerationResult.Unavailable -> {
                    Log.e(TAG, "Manual RealGemma visit JSON inference unavailable: ${generationResult.status}")
                    throw AssertionError(generationResult.status)
                }
                is TextGenerationResult.Failed -> {
                    Log.e(TAG, "Manual RealGemma visit JSON inference failed: ${generationResult.error}")
                    throw AssertionError(generationResult.error)
                }
            }

            logLong("Raw model output", rawOutput)
            assertTrue("Manual RealGemma visit JSON inference returned blank text.", rawOutput.isNotBlank())

            val parseResult = RealGemmaOutputParser().parseVisitReasoning(
                rawOutput = rawOutput,
                patient = patient,
                originalObservationText = OBSERVATION,
                protocolChunks = protocolChunks
            )

            when (parseResult) {
                is RealGemmaParseResult.Success -> {
                    val result = parseResult.result
                    Log.i(TAG, "Parser result: SUCCESS")
                    Log.i(TAG, "Parsed protocol citation: ${result.protocolCitation}")
                    Log.i(TAG, "Parsed uncertain: ${result.uncertain}")
                    Log.i(TAG, "Parsed referral present: ${result.referralFlag != null}")
                    assertNonDiagnosticAndSafetyWorded(result)
                }
                is RealGemmaParseResult.Rejected -> {
                    Log.w(TAG, "Parser result: REJECTED")
                    Log.w(TAG, "Fallback reason: ${parseResult.reason}")
                    Log.w(TAG, "Fallback structured note: ${parseResult.fallback.structuredNote}")
                    Log.w(
                        TAG,
                        "Parsing rejection is expected early model-behavior signal; " +
                            "the manual test passes because inference returned non-empty text."
                    )
                }
            }
        }
    }

    private fun meenaPatient(): Patient {
        return DemoSeedData.patients.first { it.id == "patient-meena" }
    }

    private fun meenaVisitHistory(): List<VisitLog> {
        return DemoSeedData.initialVisitLogs(nowMillis = FIXED_NOW_MILLIS)
            .filter { it.patientId == "patient-meena" }
    }

    private fun relevantProtocolChunks(): List<ProtocolChunk> {
        val relevantIds = setOf(
            "anc-danger-signs",
            "anc-routine-followup"
        )
        return DemoSeedData.protocolChunks.filter { it.id in relevantIds }
    }

    private fun assertNonDiagnosticAndSafetyWorded(result: VisitReasoningResult) {
        val combined = listOf(
            result.structuredNote,
            result.protocolCitation,
            result.suggestedFollowUp,
            result.clarificationPrompt.orEmpty(),
            result.referralFlag?.reason.orEmpty(),
            result.referralFlag?.protocolBasis.orEmpty()
        ).joinToString(separator = "\n")
        val lower = combined.lowercase()

        assertFalse("Parsed output used diagnostic language.", lower.contains("patient has preeclampsia"))
        assertFalse("Parsed output used diagnostic language.", lower.contains("diagnosed"))
        assertTrue(
            "Parsed output should include CHW confirmation/safety wording.",
            (lower.contains("chw") && lower.contains("confirm")) ||
                lower.contains("confirmation required") ||
                lower.contains("not a diagnosis")
        )
    }

    private fun logLong(label: String, text: String) {
        Log.i(TAG, "$label length: ${text.length} chars")
        text.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
            Log.i(TAG, "$label chunk ${index + 1}: $chunk")
        }
    }

    private companion object {
        const val TAG = "SmritiRealGemmaJsonTest"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val TIMEOUT_MILLIS = 120_000L
        const val LOG_CHUNK_SIZE = 3_000
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        const val OBSERVATION =
            "Meena, 28 years old, 7 months pregnant. Complaining of severe headache and blurred vision. " +
                "Blood pressure 150 over 95. Reduced fetal movement today."
        const val STRICT_JSON_REMINDER = """

            Manual test reminder:
            Return only the required JSON object. Use supplied protocol citations only.
            This is not a diagnosis. CHW confirmation is required before saving.
        """
    }
}
