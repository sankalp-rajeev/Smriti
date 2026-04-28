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
class ManualRealGemmaBenchmarkInstrumentedTest {
    @Test
    fun runsManualRealGemmaBenchmarkWithSideloadedModel() {
        runBlocking {
            val allowManualTextInference = InstrumentationRegistry.getArguments()
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true

            assumeTrue(
                "Manual RealGemma benchmark skipped: pass " +
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
                    "Manual RealGemma benchmark requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            val scenarios = benchmarkScenarios()
            val results = mutableListOf<BenchmarkResult>()
            for ((index, scenario) in scenarios.withIndex()) {
                Log.i(TAG, "Scenario ${index + 1}/${scenarios.size}: ${scenario.name}")
                results += runScenario(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    allowManualTextInference = allowManualTextInference
                )
            }

            logSummary(results)
        }
    }

    private suspend fun runScenario(
        scenario: BenchmarkScenario,
        modelStatus: ModelStatus,
        allowManualTextInference: Boolean
    ): BenchmarkResult {
        val liteRtClient = LiteRtGemmaTextClient(modelStatus = modelStatus)
        val capturingClient = CapturingManualTextClient(
            liteRtClient = liteRtClient,
            allowManualTextInference = allowManualTextInference
        )
        val agent = RealGemmaAgent(textClient = capturingClient)

        val startedAt = SystemClock.elapsedRealtime()
        val result = agent.generateVisitNote(
            patient = meenaPatient(),
            visitHistory = meenaVisitHistory(),
            observationText = scenario.observationText,
            protocolChunks = scenario.protocolChunks
        )
        val latencyMillis = SystemClock.elapsedRealtime() - startedAt

        val generation = capturingClient.generationResult
            ?: throw AssertionError("Scenario '${scenario.name}' did not call manual LiteRT inference.")

        val rawOutput = when (generation) {
            is TextGenerationResult.Success -> generation.text
            is TextGenerationResult.Unavailable -> {
                Log.e(TAG, "Scenario '${scenario.name}' inference unavailable: ${generation.status}")
                throw AssertionError(generation.status)
            }
            is TextGenerationResult.Failed -> {
                Log.e(TAG, "Scenario '${scenario.name}' inference failed: ${generation.error}")
                throw AssertionError(generation.error)
            }
        }

        if (rawOutput.isBlank()) {
            throw AssertionError("Scenario '${scenario.name}' returned blank model output.")
        }

        val parseResult = RealGemmaOutputParser().parseVisitReasoning(
            rawOutput = rawOutput,
            patient = meenaPatient(),
            originalObservationText = scenario.observationText,
            protocolChunks = scenario.protocolChunks
        )
        val rawProtocolCitation = rawOutput.extractProtocolCitation()
        val followedSingleCitationContract = if (scenario.protocolChunks.isEmpty()) {
            rawProtocolCitation == ""
        } else {
            rawProtocolCitation in scenario.protocolChunks.map { it.citation }
        }
        val parserSucceeded = parseResult is RealGemmaParseResult.Success
        val parserRejectionReason = (parseResult as? RealGemmaParseResult.Rejected)?.reason
        if (parserRejectionReason?.contains("diagnostic", ignoreCase = true) == true) {
            throw AssertionError("Scenario '${scenario.name}' parser rejected unsafe diagnostic language.")
        }

        val safetyWordingPresent = result.hasSafetyWording()
        assertTrue(
            "Scenario '${scenario.name}' missing safety wording after RealGemma safety post-processing.",
            safetyWordingPresent
        )

        val metric = BenchmarkResult(
            scenarioName = scenario.name,
            latencyMillis = latencyMillis,
            promptLength = capturingClient.promptLength,
            rawOutputLength = rawOutput.length,
            rawProtocolCitation = rawProtocolCitation,
            followedSingleCitationContract = followedSingleCitationContract,
            parserSucceeded = parserSucceeded,
            parserRejectionReason = parserRejectionReason,
            referralPresent = result.referralFlag != null,
            citationPresent = result.protocolCitation.isNotBlank(),
            safetyWordingPresent = safetyWordingPresent,
            uncertainOrClarificationPresent = result.uncertain || !result.clarificationPrompt.isNullOrBlank()
        )

        logScenarioResult(metric, result)
        return metric
    }

    private fun benchmarkScenarios(): List<BenchmarkScenario> {
        return listOf(
            BenchmarkScenario(
                name = "ANC danger signs",
                observationText = "Meena, 28 years old, 7 months pregnant. Complaining of severe headache and " +
                    "blurred vision. Blood pressure 150 over 95. Reduced fetal movement today.",
                protocolChunks = protocolChunks("anc-danger-signs", "anc-routine-followup")
            ),
            BenchmarkScenario(
                name = "Normal ANC follow-up",
                observationText = "Meena, 28 years old, 7 months pregnant. Routine ANC follow-up. " +
                    "No severe headache, no blurred vision, no bleeding, and no convulsions. " +
                    "Blood pressure 118 over 76. Fetal movement present today. Taking iron tablets.",
                protocolChunks = protocolChunks("anc-routine-followup")
            ),
            BenchmarkScenario(
                name = "Incomplete observation",
                observationText = "Meena says she feels unwell and tired today. No blood pressure recorded yet. " +
                    "Fetal movement was not asked. No other vitals available.",
                protocolChunks = emptyList()
            )
        )
    }

    private fun meenaPatient(): Patient {
        return DemoSeedData.patients.first { it.id == "patient-meena" }
    }

    private fun meenaVisitHistory(): List<VisitLog> {
        return DemoSeedData.initialVisitLogs(nowMillis = FIXED_NOW_MILLIS)
            .filter { it.patientId == "patient-meena" }
    }

    private fun protocolChunks(vararg ids: String): List<ProtocolChunk> {
        val idSet = ids.toSet()
        return DemoSeedData.protocolChunks.filter { it.id in idSet }
    }

    private fun logScenarioResult(metric: BenchmarkResult, result: VisitReasoningResult) {
        Log.i(TAG, "Scenario '${metric.scenarioName}' latencyMs=${metric.latencyMillis}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' promptLength=${metric.promptLength}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' rawOutputLength=${metric.rawOutputLength}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' rawProtocolCitation=${metric.rawProtocolCitation}")
        Log.i(
            TAG,
            "Scenario '${metric.scenarioName}' followedSingleCitationContract=" +
                metric.followedSingleCitationContract
        )
        Log.i(TAG, "Scenario '${metric.scenarioName}' parserSucceeded=${metric.parserSucceeded}")
        metric.parserRejectionReason?.let { reason ->
            Log.w(TAG, "Scenario '${metric.scenarioName}' parserRejectionReason=$reason")
        }
        Log.i(TAG, "Scenario '${metric.scenarioName}' referralPresent=${metric.referralPresent}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' citationPresent=${metric.citationPresent}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' safetyWordingPresent=${metric.safetyWordingPresent}")
        Log.i(
            TAG,
            "Scenario '${metric.scenarioName}' uncertainOrClarificationPresent=" +
                metric.uncertainOrClarificationPresent
        )
        Log.i(TAG, "Scenario '${metric.scenarioName}' protocolCitation=${result.protocolCitation}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' structuredNote=${result.structuredNote}")
        Log.i(TAG, "Scenario '${metric.scenarioName}' suggestedFollowUp=${result.suggestedFollowUp}")
        result.referralFlag?.let { referral ->
            Log.i(TAG, "Scenario '${metric.scenarioName}' referralUrgency=${referral.urgency}")
            Log.i(TAG, "Scenario '${metric.scenarioName}' referralReason=${referral.reason}")
        }
    }

    private fun logSummary(results: List<BenchmarkResult>) {
        val successCount = results.count { it.safetyWordingPresent && it.rawOutputLength > 0 }
        val parserSuccessCount = results.count { it.parserSucceeded }
        val referralCount = results.count { it.referralPresent }
        val citationCount = results.count { it.citationPresent }
        val averageLatency = results.map { it.latencyMillis }.average().toLong()
        val maxLatency = results.maxOf { it.latencyMillis }
        val uncertainCases = results
            .filter { it.uncertainOrClarificationPresent || !it.parserSucceeded }
            .joinToString(separator = ", ") { result ->
                if (result.parserRejectionReason == null) {
                    result.scenarioName
                } else {
                    "${result.scenarioName} (${result.parserRejectionReason})"
                }
            }
            .ifBlank { "none" }

        Log.i(TAG, "Benchmark summary totalScenarios=${results.size}")
        Log.i(TAG, "Benchmark summary successCount=$successCount")
        Log.i(TAG, "Benchmark summary parserSuccessCount=$parserSuccessCount")
        Log.i(TAG, "Benchmark summary referralCount=$referralCount")
        Log.i(TAG, "Benchmark summary citationCount=$citationCount")
        Log.i(
            TAG,
            "Benchmark summary singleCitationContractCount=" +
                results.count { it.followedSingleCitationContract }
        )
        Log.i(TAG, "Benchmark summary averageLatencyMs=$averageLatency")
        Log.i(TAG, "Benchmark summary maxLatencyMs=$maxLatency")
        Log.i(TAG, "Benchmark summary fallbackOrUncertainCases=$uncertainCases")
    }

    private fun VisitReasoningResult.hasSafetyWording(): Boolean {
        val lower = listOf(
            structuredNote,
            suggestedFollowUp,
            clarificationPrompt.orEmpty(),
            referralFlag?.reason.orEmpty()
        ).joinToString(separator = "\n").lowercase()
        val hasNonDiagnosticWording = lower.contains("not a diagnosis") ||
            lower.contains("no diagnosis generated")
        val hasChwConfirmation = (lower.contains("chw") && lower.contains("confirm")) ||
            lower.contains("confirmation required")
        return hasNonDiagnosticWording && hasChwConfirmation
    }

    private fun String.extractProtocolCitation(): String {
        val match = Regex("\"protocolCitation\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
            .find(this)
            ?: return "<missing>"
        return match.groupValues[1].replace("\\\"", "\"").trim()
    }

    private data class BenchmarkScenario(
        val name: String,
        val observationText: String,
        val protocolChunks: List<ProtocolChunk>
    )

    private data class BenchmarkResult(
        val scenarioName: String,
        val latencyMillis: Long,
        val promptLength: Int,
        val rawOutputLength: Int,
        val rawProtocolCitation: String,
        val followedSingleCitationContract: Boolean,
        val parserSucceeded: Boolean,
        val parserRejectionReason: String?,
        val referralPresent: Boolean,
        val citationPresent: Boolean,
        val safetyWordingPresent: Boolean,
        val uncertainOrClarificationPresent: Boolean
    )

    private class CapturingManualTextClient(
        private val liteRtClient: LiteRtGemmaTextClient,
        private val allowManualTextInference: Boolean
    ) : RealGemmaTextClient {
        var promptLength: Int = 0
            private set
        var generationResult: TextGenerationResult? = null
            private set

        override suspend fun generateText(prompt: String): TextGenerationResult {
            promptLength = prompt.length
            val result = liteRtClient.generateTextManual(
                prompt = prompt + STRICT_JSON_REMINDER,
                allowManualTextInference = allowManualTextInference,
                timeoutMillis = TIMEOUT_MILLIS
            )
            generationResult = result
            return result
        }
    }

    private companion object {
        const val TAG = "SmritiRealGemmaBenchmark"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val TIMEOUT_MILLIS = 120_000L
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        const val STRICT_JSON_REMINDER = """

            Manual benchmark reminder:
            Return only the required JSON object. Use supplied protocol citations only.
            This is not a diagnosis. CHW confirmation is required before saving.
        """
    }
}
