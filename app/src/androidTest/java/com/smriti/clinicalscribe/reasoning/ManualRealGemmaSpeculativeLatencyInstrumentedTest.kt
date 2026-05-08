package com.smriti.clinicalscribe.reasoning

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Capabilities
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalApi::class)
class ManualRealGemmaSpeculativeLatencyInstrumentedTest {
    @Test
    fun comparesCpuBaselineWithManualSpeculativeDecodingWhenExplicitlyEnabled() {
        runBlocking {
            val args = InstrumentationRegistry.getArguments()
            val allowManualTextInference = args
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true
            val allowSpeculativeDecoding = args
                .getString(ARG_ALLOW_SPECULATIVE_DECODING)
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
                "Manual speculative latency probe skipped: pass " +
                    "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_TEXT_INFERENCE=true",
                allowManualTextInference
            )
            assumeTrue(
                "Manual speculative latency probe skipped: pass " +
                    "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_SPECULATIVE_DECODING=true",
                allowSpeculativeDecoding
            )

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
            val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

            Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
            Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")

            if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
                throw AssertionError(
                    "Manual speculative latency probe requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            val scenarios = scenarios().take(scenarioLimit)
            val cpuBaseline = scenarios.map { scenario ->
                runOne(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    backendMode = LiteRtBackendMode.CPU,
                    speculativeEnabled = false
                )
            }
            assertTrue(
                "CPU baseline must return at least one successful generation.",
                cpuBaseline.any { it.generationSuccess }
            )

            val speculativeSupported = checkSpeculativeSupport(modelStatus.expectedPath)
            if (!speculativeSupported) {
                Log.w(
                    TAG,
                    "Speculative/MTP run skipped: Capabilities.hasSpeculativeDecodingSupport() returned false."
                )
                logSummary("CPU baseline", cpuBaseline)
                return@runBlocking
            }

            val cpuSpeculative = scenarios.map { scenario ->
                runOne(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    backendMode = LiteRtBackendMode.CPU,
                    speculativeEnabled = true
                )
            }

            logSummary("CPU baseline", cpuBaseline)
            logSummary("CPU speculative/MTP", cpuSpeculative)
            logComparisons(cpuBaseline, cpuSpeculative, "CPU speculative/MTP")

            if (!allowExperimentalGpu) {
                Log.i(TAG, "GPU speculative skipped: pass $ARG_ALLOW_EXPERIMENTAL_GPU_BACKEND=true.")
                return@runBlocking
            }

            val gpuSpeculative = scenarios.map { scenario ->
                runOne(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    backendMode = LiteRtBackendMode.GPU_EXPERIMENTAL,
                    speculativeEnabled = true
                )
            }
            logSummary("GPU experimental speculative/MTP", gpuSpeculative)
        }
    }

    private suspend fun runOne(
        scenario: SpeculativeScenario,
        modelStatus: ModelStatus,
        backendMode: LiteRtBackendMode,
        speculativeEnabled: Boolean
    ): SpeculativeResult {
        val prepared = LiteRtEngineConfigFactory(backendMode = backendMode)
            .prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: return SpeculativeResult(
                scenarioName = scenario.name,
                backendMode = backendMode,
                speculativeEnabled = speculativeEnabled,
                latencyMillis = 0L,
                generationSuccess = false,
                failureReason = "EngineConfig not ready."
            )

        val client = DirectSpeculativeTextClient(
            engineConfig = prepared.engineConfig,
            backendLabel = prepared.backendLabel,
            speculativeEnabled = speculativeEnabled,
            modelStatus = modelStatus
        )
        val agent = RealGemmaAgent(textClient = client, requestType = RealGemmaRequestType.MANUAL_TEST)

        val startedAt = SystemClock.elapsedRealtime()
        val visitResult = agent.generateVisitNote(
            patient = scenario.patient,
            visitHistory = scenario.history,
            observationText = scenario.observationText,
            protocolChunks = scenario.protocolChunks
        )
        val latencyMillis = SystemClock.elapsedRealtime() - startedAt
        SmritiLatencyLogger.log(
            label = "realGemmaSpeculative.${backendMode.label}.enabled=$speculativeEnabled",
            durationMillis = latencyMillis,
            scenario = scenario.name
        )

        val rawOutput = (client.generationResult as? TextGenerationResult.Success)?.text.orEmpty()
        val parseResult = if (rawOutput.isNotBlank()) {
            RealGemmaOutputParser().parseVisitReasoning(
                rawOutput = rawOutput,
                patient = scenario.patient,
                originalObservationText = scenario.observationText,
                protocolChunks = scenario.protocolChunks
            )
        } else {
            null
        }
        val parserSucceeded = parseResult is RealGemmaParseResult.Success
        val parserFailure = if (parseResult == null) {
            "No raw model output captured."
        } else {
            (parseResult as? RealGemmaParseResult.Rejected)?.reason
        }
        val citationSucceeded = visitResult.protocolCitation.isNotBlank() &&
            visitResult.protocolCitation != "No matching protocol citation"
        val safetySucceeded = visitResult.hasSafetyWording()
        val generationSuccess = client.generationResult is TextGenerationResult.Success
        val failureReason = when {
            !generationSuccess -> client.generationResult.failureReason()
            !parserSucceeded -> parserFailure
            !citationSucceeded -> "Citation validation did not produce a supplied protocol citation."
            !safetySucceeded -> "Safety wording missing after validation."
            else -> null
        }

        Log.i(
            TAG,
            "backend=${backendMode.label} speculativeMtpEnabled=$speculativeEnabled " +
                "scenario=${scenario.name} latencyMs=$latencyMillis " +
                "parserSuccess=$parserSucceeded citationValidationSuccess=$citationSucceeded " +
                "safetyValidationSuccess=$safetySucceeded failureReason=${failureReason ?: "none"}"
        )

        return SpeculativeResult(
            scenarioName = scenario.name,
            backendMode = backendMode,
            speculativeEnabled = speculativeEnabled,
            latencyMillis = latencyMillis,
            generationSuccess = generationSuccess,
            parserSucceeded = parserSucceeded,
            citationValidationSucceeded = citationSucceeded,
            safetyValidationSucceeded = safetySucceeded,
            failureReason = failureReason
        )
    }

    private fun checkSpeculativeSupport(modelPath: String): Boolean {
        val capabilities = Capabilities(modelPath)
        return try {
            capabilities.hasSpeculativeDecodingSupport().also { supported ->
                Log.i(TAG, "Capabilities.hasSpeculativeDecodingSupport=$supported")
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Speculative capability check failed: ${error.message ?: error::class.java.simpleName}")
            false
        } finally {
            capabilities.close()
        }
    }

    private fun logSummary(label: String, results: List<SpeculativeResult>) {
        val successful = results.filter { it.generationSuccess }
        val average = successful.map { it.latencyMillis }.average().takeIf { !it.isNaN() }?.toLong()
        Log.i(
            TAG,
            "$label summary total=${results.size} generationSuccess=${successful.size} " +
                "parserSuccess=${results.count { it.parserSucceeded }} " +
                "citationSuccess=${results.count { it.citationValidationSucceeded }} " +
                "safetySuccess=${results.count { it.safetyValidationSucceeded }} " +
                "averageLatencyMs=${average ?: -1}"
        )
        results.filter { it.failureReason != null }.forEach { result ->
            Log.w(
                TAG,
                "$label failure scenario=${result.scenarioName} backend=${result.backendMode.label} " +
                    "speculativeMtpEnabled=${result.speculativeEnabled} reason=${result.failureReason}"
            )
        }
    }

    private fun logComparisons(
        baseline: List<SpeculativeResult>,
        candidate: List<SpeculativeResult>,
        candidateLabel: String
    ) {
        baseline.zip(candidate)
            .filter { (cpu, speculative) -> cpu.generationSuccess && speculative.generationSuccess }
            .forEach { (cpu, speculative) ->
                val delta = speculative.latencyMillis - cpu.latencyMillis
                Log.i(
                    TAG,
                    "Compare candidate=$candidateLabel scenario=${cpu.scenarioName} " +
                        "baselineMs=${cpu.latencyMillis} candidateMs=${speculative.latencyMillis} deltaMs=$delta"
                )
                SmritiLatencyLogger.log(
                    label = "realGemmaSpeculativeDelta.${speculative.backendMode.label}",
                    durationMillis = delta,
                    scenario = cpu.scenarioName
                )
            }
    }

    private fun scenarios(): List<SpeculativeScenario> {
        return listOf(
            SpeculativeScenario(
                name = "Meena danger-sign note",
                patient = patient("patient-meena"),
                history = history("patient-meena"),
                observationText = "Meena, 28 years old, 7 months pregnant. Complaining of severe headache and " +
                    "blurred vision. Blood pressure 150 over 95. Reduced fetal movement today.",
                protocolChunks = protocolChunks("anc-danger-signs", "anc-routine-followup")
            ),
            SpeculativeScenario(
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

    private fun VisitReasoningResult.hasSafetyWording(): Boolean {
        val combined = listOf(
            structuredNote,
            suggestedFollowUp,
            clarificationPrompt.orEmpty(),
            referralFlag?.reason.orEmpty()
        ).joinToString(separator = "\n")
        if (combined.contains(PatientLanguages.Hindi.safetyWording)) {
            return true
        }
        val lower = combined.lowercase()
        val hasNonDiagnosticWording = lower.contains("not a diagnosis") ||
            lower.contains("no diagnosis generated")
        val hasChwConfirmation = (lower.contains("chw") && lower.contains("confirm")) ||
            lower.contains("confirmation required")
        return hasNonDiagnosticWording && hasChwConfirmation
    }

    private fun TextGenerationResult?.failureReason(): String? {
        return when (this) {
            is TextGenerationResult.Failed -> error
            is TextGenerationResult.Unavailable -> status
            is TextGenerationResult.Success -> null
            null -> "Text generation was not attempted."
        }
    }

    private data class SpeculativeScenario(
        val name: String,
        val patient: Patient,
        val history: List<VisitLog>,
        val observationText: String,
        val protocolChunks: List<ProtocolChunk>
    )

    private data class SpeculativeResult(
        val scenarioName: String,
        val backendMode: LiteRtBackendMode,
        val speculativeEnabled: Boolean,
        val latencyMillis: Long,
        val generationSuccess: Boolean,
        val parserSucceeded: Boolean = false,
        val citationValidationSucceeded: Boolean = false,
        val safetyValidationSucceeded: Boolean = false,
        val failureReason: String? = null
    )

    private class DirectSpeculativeTextClient(
        private val engineConfig: EngineConfig,
        private val backendLabel: String,
        private val speculativeEnabled: Boolean,
        private val modelStatus: ModelStatus
    ) : RealGemmaTextClient {
        var generationResult: TextGenerationResult? = null
            private set

        override suspend fun generateText(prompt: String): TextGenerationResult {
            return generateText(prompt, RealGemmaRequestType.MANUAL_TEST)
        }

        override suspend fun generateText(
            prompt: String,
            requestType: RealGemmaRequestType
        ): TextGenerationResult = withContext(Dispatchers.IO) {
            val lease = RealGemmaInferenceGate.tryAcquire(
                RealGemmaRequestType.MANUAL_TEST,
                RealGemmaRequestDiagnostics(
                    modelExists = true,
                    modelSizeBytes = modelStatus.fileSizeBytes,
                    sentinelExists = null,
                    backendMode = "$backendLabel speculativeMtpEnabled=$speculativeEnabled",
                    engineState = "manual_speculative_probe",
                    lastEngineFailure = RealGemmaInferenceGate.lastEngineFailure
                )
            ) ?: return@withContext TextGenerationResult.Unavailable(RealGemmaInferenceGate.BUSY_MESSAGE)
                .also { generationResult = it }

            val previousSpeculativeFlag = ExperimentalFlags.enableSpeculativeDecoding
            val result = try {
                ExperimentalFlags.enableSpeculativeDecoding = speculativeEnabled
                val output = Engine(engineConfig).use { engine ->
                    engine.initialize()
                    engine.createConversation().use { conversation ->
                        conversation.sendMessage(prompt + STRICT_JSON_REMINDER).extractText()
                    }
                }
                TextGenerationResult.Success(output)
            } catch (error: RuntimeException) {
                lease.fail(error.message ?: error::class.java.simpleName)
                TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
            } catch (error: LinkageError) {
                lease.fail(error.message ?: error::class.java.simpleName)
                TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
            } catch (error: Throwable) {
                lease.fail(error.message ?: error::class.java.simpleName)
                TextGenerationResult.Failed(error.message ?: error::class.java.simpleName)
            } finally {
                ExperimentalFlags.enableSpeculativeDecoding = previousSpeculativeFlag
                lease.release()
            }
            generationResult = result
            result
        }

        private fun com.google.ai.edge.litertlm.Message.extractText(): String {
            val text = contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString(separator = "\n") { it.text }
                .trim()
            if (text.isBlank()) {
                throw RuntimeException("LiteRT-LM response contained no text content.")
            }
            return text
        }
    }

    private companion object {
        const val TAG = "SmritiSpeculativeLatency"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val ARG_ALLOW_SPECULATIVE_DECODING = "allowSpeculativeDecoding"
        const val ARG_ALLOW_EXPERIMENTAL_GPU_BACKEND = "allowExperimentalGpuBackend"
        const val ARG_SCENARIO_LIMIT = "speculativeScenarioLimit"
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        const val STRICT_JSON_REMINDER = """

            Manual speculative latency reminder:
            Return only exact JSON with summary, referralFlag, referralReason, dangerSigns,
            followUpPlan, clarificationQuestion, citations, confidence, and safetyNote.
            Use supplied protocol citations only. This is not a diagnosis. CHW confirmation is required before saving.
        """
    }
}
