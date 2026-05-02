package com.smriti.clinicalscribe.reasoning

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.VisitLog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualRealGemmaMemoryStressInstrumentedTest {
    @Test
    fun runsManualRealGemmaMemoryStressWithSideloadedModel() {
        runBlocking {
            val allowManualTextInference = InstrumentationRegistry.getArguments()
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true

            assumeTrue(
                "Manual RealGemma memory stress skipped: pass " +
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
                    "Manual RealGemma memory stress requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            val results = CONTEXT_SIZES.map { visitCount ->
                runStressScenario(
                    visitCount = visitCount,
                    modelStatus = modelStatus,
                    allowManualTextInference = allowManualTextInference
                )
            }
            logSummary(results)
        }
    }

    private suspend fun runStressScenario(
        visitCount: Int,
        modelStatus: ModelStatus,
        allowManualTextInference: Boolean
    ): MemoryStressResult {
        val liteRtClient = LiteRtGemmaTextClient(modelStatus = modelStatus)
        val capturingClient = CapturingManualTextClient(liteRtClient, allowManualTextInference)
        val agent = RealGemmaAgent(
            textClient = capturingClient,
            promptBuilder = RealGemmaPromptBuilder(
                maxHistoryVisits = visitCount,
                historyFormatter = RealGemmaHistoryFormatter.Compact
            )
        )
        val memoryBefore = usedMemoryBytes()
        val startedAt = SystemClock.elapsedRealtime()
        val result = agent.generateVisitNote(
            patient = DemoSeedData.patients.first { it.id == "patient-meena" },
            visitHistory = syntheticVisitHistory(visitCount),
            observationText = OBSERVATION,
            protocolChunks = DemoSeedData.protocolChunks.filter { it.id == "anc-danger-signs" || it.id == "anc-routine-followup" }
        )
        val latencyMillis = SystemClock.elapsedRealtime() - startedAt
        val memoryAfter = usedMemoryBytes()

        val rawOutput = when (val generation = capturingClient.generationResult) {
            is TextGenerationResult.Success -> generation.text
            is TextGenerationResult.Unavailable -> {
                Log.e(TAG, "Memory stress $visitCount unavailable: ${generation.status}")
                throw AssertionError(generation.status)
            }
            is TextGenerationResult.Failed -> {
                Log.e(TAG, "Memory stress $visitCount failed: ${generation.error}")
                throw AssertionError(generation.error)
            }
            null -> throw AssertionError("Memory stress $visitCount did not call manual LiteRT inference.")
        }
        if (rawOutput.isBlank()) {
            throw AssertionError("Memory stress $visitCount returned blank model output.")
        }

        val parseResult = RealGemmaOutputParser().parseVisitReasoning(
            rawOutput = rawOutput,
            patient = DemoSeedData.patients.first { it.id == "patient-meena" },
            originalObservationText = OBSERVATION,
            protocolChunks = DemoSeedData.protocolChunks.filter { it.id == "anc-danger-signs" || it.id == "anc-routine-followup" }
        )
        val parserSucceeded = parseResult is RealGemmaParseResult.Success
        val rejectionReason = (parseResult as? RealGemmaParseResult.Rejected)?.reason
        val failureCategory = rejectionReason?.toFailureCategory()
        if (rejectionReason?.contains("diagnostic", ignoreCase = true) == true) {
            throw AssertionError("Memory stress $visitCount parser rejected unsafe diagnostic language.")
        }
        if (!parserSucceeded) {
            Log.w(TAG, "Context $visitCount parserFailureCategory=$failureCategory")
            Log.w(TAG, "Context $visitCount invalidRawFirst500=${rawOutput.firstForLog()}")
            Log.w(TAG, "Context $visitCount invalidRawLast500=${rawOutput.lastForLog()}")
        }

        assertTrue(
            "Memory stress $visitCount missing safety wording after post-processing.",
            result.hasSafetyWording()
        )

        val metric = MemoryStressResult(
            visitCount = visitCount,
            promptLength = capturingClient.promptLength,
            latencyMillis = latencyMillis,
            rawOutputLength = rawOutput.length,
            parserSucceeded = parserSucceeded,
            rejectionReason = rejectionReason,
            failureCategory = failureCategory,
            citationPresent = result.protocolCitation.isNotBlank(),
            referralPresent = result.referralFlag != null,
            uncertainPresent = result.uncertain || !result.clarificationPrompt.isNullOrBlank(),
            safetyWordingPresent = result.hasSafetyWording(),
            memoryBeforeBytes = memoryBefore,
            memoryAfterBytes = memoryAfter
        )
        logResult(metric)
        return metric
    }

    private fun syntheticVisitHistory(count: Int): List<VisitLog> {
        val dayMillis = 24L * 60L * 60L * 1000L
        return (1..count).map { index ->
            val bpSystolic = 112 + (index % 8)
            val bpDiastolic = 72 + (index % 6)
            VisitLog(
                patientId = "patient-meena",
                visitDateMillis = FIXED_NOW_MILLIS - (index * 4L * dayMillis),
                observationText = "Compressed prior ANC contact $index. BP $bpSystolic/$bpDiastolic. Fetal movement present. No danger signs reported.",
                structuredNote = "Visit $index: BP $bpSystolic/$bpDiastolic, fetal movement present, iron adherence discussed, no severe headache, no blurred vision, no bleeding, no convulsions.",
                protocolCitation = "WHO ANC Contact schedule",
                suggestedFollowUp = "Continue routine ANC follow-up and review danger signs.",
                confirmed = true
            )
        }
    }

    private fun logResult(metric: MemoryStressResult) {
        Log.i(TAG, "Context visits=${metric.visitCount}")
        Log.i(TAG, "Context ${metric.visitCount} promptLength=${metric.promptLength}")
        Log.i(TAG, "Context ${metric.visitCount} latencyMs=${metric.latencyMillis}")
        Log.i(TAG, "Context ${metric.visitCount} rawOutputLength=${metric.rawOutputLength}")
        Log.i(TAG, "Context ${metric.visitCount} parserSucceeded=${metric.parserSucceeded}")
        metric.rejectionReason?.let { Log.w(TAG, "Context ${metric.visitCount} parserRejectionReason=$it") }
        metric.failureCategory?.let { Log.w(TAG, "Context ${metric.visitCount} parserFailureCategory=$it") }
        Log.i(TAG, "Context ${metric.visitCount} citationPresent=${metric.citationPresent}")
        Log.i(TAG, "Context ${metric.visitCount} safetyWordingPresent=${metric.safetyWordingPresent}")
        Log.i(TAG, "Context ${metric.visitCount} referralPresent=${metric.referralPresent}")
        Log.i(TAG, "Context ${metric.visitCount} uncertainPresent=${metric.uncertainPresent}")
        Log.i(TAG, "Context ${metric.visitCount} memoryBeforeBytes=${metric.memoryBeforeBytes}")
        Log.i(TAG, "Context ${metric.visitCount} memoryAfterBytes=${metric.memoryAfterBytes}")
        Log.i(TAG, "Context ${metric.visitCount} memoryDeltaBytes=${metric.memoryAfterBytes - metric.memoryBeforeBytes}")
    }

    private fun logSummary(results: List<MemoryStressResult>) {
        Log.i(TAG, "Memory summary contextSizes=${results.joinToString { it.visitCount.toString() }}")
        Log.i(TAG, "Memory summary parserSuccessCount=${results.count { it.parserSucceeded }}")
        Log.i(
            TAG,
            "Memory summary failureCategories=" +
                results.filter { !it.parserSucceeded }.joinToString { "${it.visitCount}:${it.failureCategory}" }.ifBlank { "none" }
        )
        Log.i(TAG, "Memory summary citationCount=${results.count { it.citationPresent }}")
        Log.i(TAG, "Memory summary maxLatencyMs=${results.maxOf { it.latencyMillis }}")
        Log.i(TAG, "Memory summary maxPromptLength=${results.maxOf { it.promptLength }}")
        Log.i(TAG, "Memory summary maxMemoryDeltaBytes=${results.maxOf { it.memoryAfterBytes - it.memoryBeforeBytes }}")
    }

    private fun usedMemoryBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun String.toFailureCategory(): String {
        val lower = lowercase()
        return when {
            lower.contains("invalid json") || lower.contains("not a single compact json") -> "malformed_json"
            lower.contains("citation") || lower.contains("protocol") || lower.contains("grounded") -> "citation_contract"
            lower.contains("diagnostic") -> "safety_diagnostic"
            else -> "other"
        }
    }

    private fun String.firstForLog(maxLength: Int = 500): String {
        return replace(Regex("\\s+"), " ").trim().take(maxLength)
    }

    private fun String.lastForLog(maxLength: Int = 500): String {
        return replace(Regex("\\s+"), " ").trim().takeLast(maxLength)
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

    private data class MemoryStressResult(
        val visitCount: Int,
        val promptLength: Int,
        val latencyMillis: Long,
        val rawOutputLength: Int,
        val parserSucceeded: Boolean,
        val rejectionReason: String?,
        val failureCategory: String?,
        val citationPresent: Boolean,
        val referralPresent: Boolean,
        val uncertainPresent: Boolean,
        val safetyWordingPresent: Boolean,
        val memoryBeforeBytes: Long,
        val memoryAfterBytes: Long
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
        const val TAG = "SmritiRealGemmaMemory"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val TIMEOUT_MILLIS = 120_000L
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        val CONTEXT_SIZES = listOf(10, 20, 40)
        const val OBSERVATION =
            "Meena, 28 years old, 7 months pregnant. Complaining of severe headache and blurred vision. " +
                "Blood pressure 150 over 95. Reduced fetal movement today."
        const val STRICT_JSON_REMINDER = """

            Manual memory stress reminder:
            Return exact JSON with summary, referralFlag, referralReason, dangerSigns,
            followUpPlan, clarificationQuestion, citations, confidence, and safetyNote.
            The first character must be { and the last character must be }.
            Do not use markdown, code fences, prefaces, explanations, repeated JSON objects, or bullet lists outside JSON.
            Do not add trailing commas or extra keys.
            Keep JSON string values short and single-line; avoid newline characters inside JSON string values.
            Keep measurements such as BP 150/95 inside strings; do not emit standalone JSON number fields.
            referralFlag must be true/false without quotes. citations, dangerSigns, and followUpPlan must be arrays.
            Use supplied protocol citations only. Keep the entire JSON under 900 characters.
            This is not a diagnosis. CHW confirmation is required before saving.
        """
    }
}
