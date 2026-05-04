package com.smriti.clinicalscribe.reasoning

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualRealGemmaBackendLatencyInstrumentedTest {
    @Test
    fun comparesCpuWithExperimentalGpuWhenExplicitlyEnabled() {
        runBlocking {
            val args = InstrumentationRegistry.getArguments()
            val allowManualTextInference = args
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true
            val allowExperimentalGpu = args
                .getString(ARG_ALLOW_EXPERIMENTAL_GPU_BACKEND)
                ?.equals("true", ignoreCase = true) == true
            val scenarioLimit = args
                .getString(ARG_SCENARIO_LIMIT)
                ?.toIntOrNull()
                ?.coerceIn(1, 2)
                ?: 1

            assumeTrue(
                "Manual backend latency test skipped: pass " +
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
                    "Manual backend latency test requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            val scenarios = scenarios().take(scenarioLimit)
            val cpuResults = scenarios.map { scenario ->
                runOne(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    backendMode = LiteRtBackendMode.CPU,
                    allowManualTextInference = allowManualTextInference
                )
            }
            assertTrue("CPU baseline must return at least one successful generation.", cpuResults.any { it.success })

            if (!allowExperimentalGpu) {
                Log.i(TAG, "Experimental GPU backend skipped: pass $ARG_ALLOW_EXPERIMENTAL_GPU_BACKEND=true.")
                logSummary("CPU baseline", cpuResults)
                return@runBlocking
            }

            val gpuResults = scenarios.map { scenario ->
                runOne(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    backendMode = LiteRtBackendMode.GPU_EXPERIMENTAL,
                    allowManualTextInference = allowManualTextInference
                )
            }
            logSummary("CPU baseline", cpuResults)
            logSummary("GPU experimental", gpuResults)

            val successfulPairs = cpuResults.zip(gpuResults)
                .filter { (_, gpu) -> gpu.success }
            if (successfulPairs.isEmpty()) {
                Log.w(TAG, "GPU experimental returned no successful generations; keep CPU as stable backend.")
                return@runBlocking
            }

            successfulPairs.forEach { (cpu, gpu) ->
                val delta = gpu.latencyMillis - cpu.latencyMillis
                Log.i(
                    TAG,
                    "Compare scenario=${cpu.scenarioName} cpuMs=${cpu.latencyMillis} " +
                        "gpuMs=${gpu.latencyMillis} deltaMs=$delta"
                )
                SmritiLatencyLogger.log(
                    label = "realGemmaBackendCompare.experimentalGpuDelta",
                    durationMillis = delta,
                    scenario = cpu.scenarioName
                )
            }
        }
    }

    private suspend fun runOne(
        scenario: BackendScenario,
        modelStatus: ModelStatus,
        backendMode: LiteRtBackendMode,
        allowManualTextInference: Boolean
    ): BackendResult {
        val prompt = RealGemmaPromptBuilder().buildVisitReasoningPrompt(
            patient = scenario.patient,
            visitHistory = scenario.history,
            observationText = scenario.observationText,
            protocolChunks = scenario.protocolChunks
        ) + STRICT_JSON_REMINDER
        val client = LiteRtGemmaTextClient(
            modelStatus = modelStatus,
            engineConfigFactory = LiteRtEngineConfigFactory(backendMode = backendMode)
        )

        val startedAt = SystemClock.elapsedRealtime()
        val generation = client.generateTextManual(
            prompt = prompt,
            allowManualTextInference = allowManualTextInference,
            timeoutMillis = TIMEOUT_MILLIS
        )
        val latencyMillis = SystemClock.elapsedRealtime() - startedAt
        SmritiLatencyLogger.log(
            label = "realGemmaBackendExperiment.${backendMode.label}",
            durationMillis = latencyMillis,
            scenario = scenario.name
        )

        return when (generation) {
            is TextGenerationResult.Success -> {
                Log.i(
                    TAG,
                    "Backend=${backendMode.label} scenario=${scenario.name} " +
                        "latencyMs=$latencyMillis outputChars=${generation.text.length}"
                )
                BackendResult(scenario.name, backendMode, latencyMillis, success = true)
            }
            is TextGenerationResult.Unavailable -> {
                Log.w(
                    TAG,
                    "Backend=${backendMode.label} scenario=${scenario.name} unavailable: ${generation.status}"
                )
                BackendResult(scenario.name, backendMode, latencyMillis, success = false, reason = generation.status)
            }
            is TextGenerationResult.Failed -> {
                Log.w(
                    TAG,
                    "Backend=${backendMode.label} scenario=${scenario.name} failed: ${generation.error}"
                )
                BackendResult(scenario.name, backendMode, latencyMillis, success = false, reason = generation.error)
            }
        }
    }

    private fun logSummary(label: String, results: List<BackendResult>) {
        val successful = results.filter { it.success }
        val average = successful.map { it.latencyMillis }.average().takeIf { !it.isNaN() }?.toLong()
        Log.i(
            TAG,
            "$label summary total=${results.size} success=${successful.size} " +
                "averageLatencyMs=${average ?: -1} failures=${results.count { !it.success }}"
        )
        results.filterNot { it.success }.forEach { result ->
            Log.w(TAG, "$label failure scenario=${result.scenarioName} reason=${result.reason}")
        }
    }

    private fun scenarios(): List<BackendScenario> {
        return listOf(
            BackendScenario(
                name = "Meena danger-sign note",
                patient = patient("patient-meena"),
                history = history("patient-meena"),
                observationText = "Meena, 28 years old, 7 months pregnant. Complaining of severe headache and " +
                    "blurred vision. Blood pressure 150 over 95. Reduced fetal movement today.",
                protocolChunks = protocolChunks("anc-danger-signs", "anc-routine-followup")
            ),
            BackendScenario(
                name = "Lucia Spanish routine note",
                patient = patient("patient-lucia"),
                history = history("patient-lucia"),
                observationText = "Lucia reports routine ANC follow-up today. No severe headache, no bleeding, " +
                    "no convulsions, fetal movement present, and blood pressure is 112 over 72.",
                protocolChunks = protocolChunks("anc-routine-followup")
            )
        )
    }

    private fun patient(id: String): Patient {
        return DemoSeedData.patients.first { it.id == id }
    }

    private fun history(patientId: String): List<VisitLog> {
        return DemoSeedData.initialVisitLogs(nowMillis = FIXED_NOW_MILLIS)
            .filter { it.patientId == patientId }
    }

    private fun protocolChunks(vararg ids: String): List<ProtocolChunk> {
        val idSet = ids.toSet()
        return DemoSeedData.protocolChunks.filter { it.id in idSet }
    }

    private data class BackendScenario(
        val name: String,
        val patient: Patient,
        val history: List<VisitLog>,
        val observationText: String,
        val protocolChunks: List<ProtocolChunk>
    )

    private data class BackendResult(
        val scenarioName: String,
        val backendMode: LiteRtBackendMode,
        val latencyMillis: Long,
        val success: Boolean,
        val reason: String? = null
    )

    private companion object {
        const val TAG = "SmritiBackendLatency"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val ARG_ALLOW_EXPERIMENTAL_GPU_BACKEND = "allowExperimentalGpuBackend"
        const val ARG_SCENARIO_LIMIT = "backendScenarioLimit"
        const val TIMEOUT_MILLIS = 120_000L
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        const val STRICT_JSON_REMINDER = """

            Backend latency experiment reminder:
            Return only exact JSON with summary, referralFlag, referralReason, dangerSigns,
            followUpPlan, clarificationQuestion, citations, confidence, and safetyNote.
            Use supplied protocol citations only. This is not a diagnosis. CHW confirmation is required before saving.
        """
    }
}
