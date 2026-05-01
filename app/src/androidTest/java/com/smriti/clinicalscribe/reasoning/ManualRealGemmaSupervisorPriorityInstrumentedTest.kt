package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.PatientMemoryInsights
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualRealGemmaSupervisorPriorityInstrumentedTest {
    @Test
    fun runsManualRealGemmaSupervisorPriorityQueueWithSideloadedModel() {
        runBlocking {
            val allowManualTextInference = InstrumentationRegistry.getArguments()
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true

            assumeTrue(
                "Manual RealGemma supervisor priority skipped: pass " +
                    "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_TEXT_INFERENCE=true",
                allowManualTextInference
            )

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
            val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

            Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
            Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

            if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
                throw AssertionError(
                    "Manual RealGemma supervisor priority test requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            val visits = DemoSeedData.initialVisitLogs(nowMillis = FIXED_NOW_MILLIS)
            val patients = DemoSeedData.patients
            val missedFollowUps = PatientMemoryInsights.missedFollowUpAlerts(
                patientId = "patient-amara",
                visits = visits,
                nowMillis = FIXED_NOW_MILLIS
            )
            val historySignals = patients.mapNotNull { patient ->
                PatientMemoryInsights.risingBloodPressureSignal(patient, visits)
            }
            val liteRtClient = LiteRtGemmaTextClient(modelStatus = modelStatus)
            val textClient = ManualPriorityTextClient(liteRtClient, allowManualTextInference)
            val generator = SupervisorPriorityQueueGenerator(textClient = textClient)

            val result = generator.generate(
                patients = patients,
                todayVisits = visits.filter { it.confirmed },
                referrals = emptyList(),
                missedFollowUps = missedFollowUps,
                historySignals = historySignals
            )

            when (result) {
                is SupervisorPriorityQueueResult.Available -> {
                    Log.i(TAG, "Priority queue size: ${result.queue.items.size}")
                    result.queue.items.forEachIndexed { index, item ->
                        Log.i(
                            TAG,
                            "Priority ${index + 1}: ${item.patientName}; urgency=${item.urgency}; basis=${item.protocolBasis}"
                        )
                        assertTrue(
                            "Priority item must include non-diagnostic safety wording.",
                            item.nonDiagnosticSafety.lowercase().contains("not a diagnosis")
                        )
                    }
                }

                is SupervisorPriorityQueueResult.Unavailable -> {
                    Log.e(TAG, "Manual RealGemma supervisor priority unavailable: ${result.reason}")
                    throw AssertionError(result.reason)
                }
            }

            assertTrue("LiteRtGemmaTextClient did not attempt model loading.", liteRtClient.modelLoadAttempted)
            assertTrue("LiteRtGemmaTextClient did not initialize the engine.", liteRtClient.engineInitializationAttempted)
            assertTrue("LiteRtGemmaTextClient did not create a conversation.", liteRtClient.conversationCreated)
            assertTrue("LiteRtGemmaTextClient did not attempt inference.", liteRtClient.inferenceAttempted)
        }
    }

    private class ManualPriorityTextClient(
        private val liteRtClient: LiteRtGemmaTextClient,
        private val allowManualTextInference: Boolean
    ) : RealGemmaTextClient {
        override suspend fun generateText(prompt: String): TextGenerationResult {
            Log.i(TAG, "Supervisor priority prompt length: ${prompt.length} chars")
            prompt.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
                Log.i(TAG, "Prompt chunk ${index + 1}: $chunk")
            }
            val result = liteRtClient.generateTextManual(
                prompt = prompt + STRICT_JSON_REMINDER,
                allowManualTextInference = allowManualTextInference,
                timeoutMillis = TIMEOUT_MILLIS
            )
            when (result) {
                is TextGenerationResult.Success -> {
                    Log.i(TAG, "Manual supervisor priority output length: ${result.text.length} chars")
                    result.text.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
                        Log.i(TAG, "Raw output chunk ${index + 1}: $chunk")
                    }
                }

                is TextGenerationResult.Unavailable -> Log.e(TAG, "Manual supervisor priority unavailable: ${result.status}")
                is TextGenerationResult.Failed -> Log.e(TAG, "Manual supervisor priority failed: ${result.error}")
            }
            return result
        }
    }

    private companion object {
        const val TAG = "SmritiRealGemmaPriority"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val TIMEOUT_MILLIS = 120_000L
        const val LOG_CHUNK_SIZE = 3_000
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        const val STRICT_JSON_REMINDER = """

            Manual supervisor priority reminder:
            Return exactly one compact JSON object and nothing else.
            The first character must be { and the last character must be }.
            Use the required {"items":[...]} shape only.
            Rank at most 5 items.
            Use urgency IMMEDIATE, WITHIN_24H, or ROUTINE only.
            Use supplied protocol citations only, or an empty protocolBasis.
            Do not diagnose. nonDiagnosticSafety must include "not a diagnosis".
        """
    }
}
