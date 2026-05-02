package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualRealGemmaMultilingualInstrumentedTest {
    @Test
    fun runsManualMultilingualRealGemmaScenariosWithSideloadedModel() {
        runBlocking {
            val allowManualTextInference = InstrumentationRegistry.getArguments()
                .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
                ?.equals("true", ignoreCase = true) == true

            assumeTrue(
                "Manual multilingual RealGemma skipped: pass " +
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
                    "Manual multilingual RealGemma test requires sideloaded model at ${expectedModelFile.absolutePath}."
                )
            }

            scenarios().forEach { scenario ->
                runScenario(
                    scenario = scenario,
                    modelStatus = modelStatus,
                    allowManualTextInference = allowManualTextInference
                )
            }
        }
    }

    private suspend fun runScenario(
        scenario: MultilingualScenario,
        modelStatus: ModelStatus,
        allowManualTextInference: Boolean
    ) {
        val liteRtClient = LiteRtGemmaTextClient(modelStatus = modelStatus)
        val client = CapturingManualTextClient(liteRtClient, allowManualTextInference)
        val agent = RealGemmaAgent(textClient = client)
        val result = agent.generateVisitNote(
            patient = scenario.patient,
            visitHistory = scenario.history,
            observationText = scenario.observation,
            protocolChunks = scenario.protocolChunks
        )
        val language = PatientLanguages.forPatient(scenario.patient)
        val rawOutput = client.rawOutput.orEmpty()
        val parserStatus = if (result.uncertain && result.structuredNote.contains("output rejected", ignoreCase = true)) {
            "rejected_or_uncertain"
        } else {
            "parsed_or_post_processed"
        }
        val heuristicMatch = appearsToUseRequestedLanguage(
            text = listOf(rawOutput, result.structuredNote, result.suggestedFollowUp).joinToString("\n"),
            languageCode = language.code
        )

        Log.i(TAG, "Scenario patient=${scenario.patient.name}; language=${language.englishName} (${language.code})")
        Log.i(TAG, "Scenario parserStatus=$parserStatus")
        Log.i(TAG, "Scenario heuristicLanguageMatch=$heuristicMatch")
        Log.i(TAG, "Scenario protocolCitation=${result.protocolCitation}")
        Log.i(TAG, "Scenario safetyPresent=${result.structuredNote.contains(language.safetyWording)}")
        Log.i(TAG, "Raw output preview=${rawOutput.replace(Regex("\\s+"), " ").take(500)}")

        assertTrue(
            "Safety wording missing for ${language.englishName}",
            result.structuredNote.contains(language.safetyWording)
        )
        assertTrue("Expected protocol citation for ${scenario.patient.name}", result.protocolCitation.isNotBlank())
        assertTrue("LiteRT inference was not attempted.", liteRtClient.inferenceAttempted)
    }

    private fun scenarios(): List<MultilingualScenario> {
        val patients = DemoSeedData.patients.associateBy { it.id }
        val visits = DemoSeedData.initialVisitLogs(nowMillis = FIXED_NOW_MILLIS)
        val protocols = DemoSeedData.protocolChunks.filter { it.id == "anc-danger-signs" || it.id == "anc-routine-followup" }
        return listOf(
            MultilingualScenario(
                patient = patients.getValue("patient-meena"),
                history = visits.filter { it.patientId == "patient-meena" },
                observation = "Meena reports severe headache, blurred vision, BP 150/95, and reduced fetal movement.",
                protocolChunks = protocols
            ),
            MultilingualScenario(
                patient = patients.getValue("patient-grace"),
                history = visits.filter { it.patientId == "patient-grace" },
                observation = "Grace reports routine ANC visit, fetal movement present, no bleeding, BP 116/74.",
                protocolChunks = protocols
            ),
            MultilingualScenario(
                patient = patients.getValue("patient-lucia"),
                history = visits.filter { it.patientId == "patient-lucia" },
                observation = "Lucia reports routine ANC visit, fetal movement present, no danger signs, BP 114/72.",
                protocolChunks = protocols
            )
        )
    }

    private fun appearsToUseRequestedLanguage(text: String, languageCode: String): Boolean {
        val lower = text.lowercase()
        return when (languageCode) {
            "hi" -> text.any { it in '\u0900'..'\u097F' }
            "sw" -> listOf("mgonjwa", "afya", "uchunguzi", "dokezo", "rufaa").any { it in lower }
            "es" -> listOf("paciente", "salud", "seguimiento", "nota", "diagnóstico").any { it in lower }
            else -> true
        }
    }

    private data class MultilingualScenario(
        val patient: Patient,
        val history: List<VisitLog>,
        val observation: String,
        val protocolChunks: List<ProtocolChunk>
    )

    private class CapturingManualTextClient(
        private val liteRtClient: LiteRtGemmaTextClient,
        private val allowManualTextInference: Boolean
    ) : RealGemmaTextClient {
        var rawOutput: String? = null
            private set

        override suspend fun generateText(prompt: String): TextGenerationResult {
            Log.i(TAG, "Prompt length=${prompt.length}")
            val result = liteRtClient.generateTextManual(
                prompt = prompt + "\n\n" + strictJsonReminder(),
                allowManualTextInference = allowManualTextInference,
                timeoutMillis = TIMEOUT_MILLIS
            )
            rawOutput = (result as? TextGenerationResult.Success)?.text
            return result
        }
    }

    private companion object {
        const val TAG = "SmritiRealGemmaLang"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val TIMEOUT_MILLIS = 120_000L
        const val FIXED_NOW_MILLIS = 1_772_496_000_000L
        fun strictJsonReminder(): String = """

            Manual multilingual reminder:
            Return exactly one compact JSON object and nothing else.
            Use the requested patient output language for user-facing fields.
            Keep protocolCitation and referralFlag.protocolBasis as supplied citation strings.
            Include the required safety wording in the requested patient output language.
        """
    }
}
