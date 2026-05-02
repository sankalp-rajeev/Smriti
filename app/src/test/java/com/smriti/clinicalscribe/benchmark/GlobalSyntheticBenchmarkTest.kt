package com.smriti.clinicalscribe.benchmark

import com.smriti.clinicalscribe.reasoning.AgentConfig
import com.smriti.clinicalscribe.reasoning.AgentMode
import com.smriti.clinicalscribe.reasoning.ModelAvailability
import com.smriti.clinicalscribe.reasoning.RealGemmaDeveloperMode
import com.smriti.clinicalscribe.rag.ProtocolRegion
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSyntheticBenchmarkTest {
    private val cases = GlobalSyntheticBenchmarkCases.cases
    private val retriever = ProtocolRetriever.fromJson(assetCorpusJson())
    private val runner = SyntheticBenchmarkRunner(retriever)

    @Test
    fun datasetHasAtLeastTenSyntheticCases() {
        assertTrue(cases.size >= 10)
        assertTrue(cases.all { it.id.isNotBlank() })
        assertEquals(cases.size, cases.map { it.id }.toSet().size)
    }

    @Test
    fun datasetRepresentsRequiredCountriesAndRegions() {
        val countries = cases.mapNotNull { it.countryCode }.toSet()
        val regions = cases.map { it.region }.toSet()

        assertTrue(countries.contains("IN"))
        assertTrue(countries.contains("BD"))
        assertTrue(countries.contains("ET"))
        assertTrue(regions.contains(ProtocolRegion.GLOBAL_CORE.name))
        assertTrue(regions.contains(ProtocolRegion.INDIA.name))
        assertTrue(regions.contains(ProtocolRegion.BANGLADESH.name))
        assertTrue(regions.contains(ProtocolRegion.ETHIOPIA.name))
        assertTrue(regions.contains(ProtocolRegion.AFRICA_REGION.name))
        assertTrue(regions.contains(ProtocolRegion.SOUTH_AMERICA_REGION.name))
    }

    @Test
    fun everyCaseDeclaresExpectationsAndNotes() {
        cases.forEach { case ->
            assertTrue(case.title.isNotBlank())
            assertTrue(case.patientContext.isNotBlank())
            assertTrue(case.transcript.isNotBlank())
            assertTrue(case.expectedCitationPrefix.isNotBlank())
            assertTrue(case.notes.isNotBlank())
        }
    }

    @Test
    fun mockBenchmarkRunnerPassesAllCases() = runBlocking {
        val results = cases.map { runner.run(it) }
        val failures = results.filterNot { it.pass }

        assertTrue(
            failures.joinToString(separator = "\n") { "${it.case.id}: ${it.reason}" },
            failures.isEmpty()
        )
    }

    @Test
    fun dangerSignCasesTriggerReferral() = runBlocking {
        val results = cases
            .filter { it.expectedReferralRequired }
            .map { runner.run(it) }

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.referralRequired })
    }

    @Test
    fun routineCasesDoNotCreateFalseReferral() = runBlocking {
        val results = cases
            .filterNot { it.expectedReferralRequired }
            .map { runner.run(it) }

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { !it.referralRequired })
    }

    @Test
    fun vagueCaseRemainsUncertainAndAsksForClarification() = runBlocking {
        val result = runner.run(cases.single { it.id == "vague_incomplete_observation" })

        assertTrue(result.uncertain)
        assertTrue(result.clarificationPrompt!!.contains("confirm", ignoreCase = true))
        assertEquals(ExpectedRetrievalLevel.NONE, result.selectedRetrievalLevel)
    }

    @Test
    fun fallbackCasesRetrieveExpectedRegionOrGlobalChunks() = runBlocking {
        val africa = runner.run(cases.single { it.id == "africa_region_fallback" })
        val southAmerica = runner.run(cases.single { it.id == "south_america_region_fallback" })
        val global = runner.run(cases.single { it.id == "global_core_fallback" })

        assertEquals(ExpectedRetrievalLevel.REGION, africa.selectedRetrievalLevel)
        assertEquals(ExpectedRetrievalLevel.REGION, southAmerica.selectedRetrievalLevel)
        assertEquals(ExpectedRetrievalLevel.GLOBAL_CORE, global.selectedRetrievalLevel)
    }

    @Test
    fun existingMeenaDemoBehaviorIsPreserved() = runBlocking {
        val result = runner.run(cases.single { it.id == "india_anc_danger_signs" })

        assertTrue(result.pass)
        assertEquals(ExpectedRetrievalLevel.EXACT_COUNTRY, result.selectedRetrievalLevel)
        assertTrue(result.retrievedCitations.first().startsWith("Smriti Demo Maternal Health Protocol"))
        assertTrue(result.referralRequired)
    }

    @Test
    fun realGemmaRequiredIsDefaultAndNoDeveloperGateFallsBackToMock() {
        val filesDir = Files.createTempDirectory("smriti-benchmark-missing-model").toFile()
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()

        val defaultStatus = RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = false,
            localGateEnabled = false,
            modelStatus = modelStatus
        )
        val oneGateStatus = RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = false,
            modelStatus = modelStatus
        )

        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, AgentConfig.DEFAULT_MODE)
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, defaultStatus.activeAgentMode)
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, oneGateStatus.activeAgentMode)
        assertTrue(defaultStatus.usesRealGemmaVisitAgent)
        assertTrue(oneGateStatus.usesRealGemmaVisitAgent)
    }

    private fun assetCorpusJson(): String {
        val modulePath = File("src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        val rootPath = File("app/src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        return when {
            modulePath.exists() -> modulePath.readText()
            else -> rootPath.readText()
        }
    }
}
