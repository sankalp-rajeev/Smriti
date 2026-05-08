package com.smriti.clinicalscribe.ui

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolRegion
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrgentProtocolLookupBuilderTest {
    private val retriever = ProtocolRetriever.fromJson(assetCorpusJson())

    @Test
    fun dangerSignChipsRetrieveLocalProtocolWithCitation() {
        val meena = DemoSeedData.patients.first { it.id == "patient-meena" }

        val result = UrgentProtocolLookupBuilder.lookup(
            selectedSigns = signs("Severe headache", "Blurred vision"),
            freeText = "",
            patient = meena,
            retriever = retriever
        )

        assertTrue(result.hasGuidance)
        assertTrue(result.urgentReviewMayBeNeeded)
        assertEquals("mh_severe_headache", result.guidanceChunk?.id)
        assertTrue(result.guidanceChunk?.citation.orEmpty().isNotBlank())
        assertTrue(result.observedSigns.contains("Severe headache"))
        assertTrue(result.observedSigns.contains("Blurred vision"))
    }

    @Test
    fun exactCountryContextOutranksRegionAndGlobalGuidance() {
        val localRetriever = ProtocolRetriever.fromJson(countryRankingJson())
        val meena = DemoSeedData.patients.first { it.id == "patient-meena" }

        val result = UrgentProtocolLookupBuilder.lookup(
            selectedSigns = emptyList(),
            freeText = "shared urgent marker",
            patient = meena,
            retriever = localRetriever
        )

        assertEquals("exact_country", result.guidanceChunk?.id)
        assertTrue(result.urgentReviewMayBeNeeded)
        assertTrue(result.contextLabel.contains("India"))
    }

    @Test
    fun globalFallbackWorksWithoutPatientContext() {
        val result = UrgentProtocolLookupBuilder.lookup(
            selectedSigns = signs("Severe headache", "Blurred vision"),
            freeText = "",
            patient = null,
            retriever = retriever
        )

        assertNotNull(result.guidanceChunk)
        assertEquals(ProtocolRegion.GLOBAL_CORE.name, result.guidanceChunk?.region)
        assertEquals("Global local guidance", result.contextLabel)
        assertTrue(result.guidanceChunk?.citation.orEmpty().contains("WHO"))
    }

    @Test
    fun unmatchedObservationReturnsSafeNoGuidanceState() {
        val result = UrgentProtocolLookupBuilder.lookup(
            selectedSigns = emptyList(),
            freeText = "broken water pump near the school",
            patient = null,
            retriever = retriever
        )

        assertFalse(result.hasGuidance)
        assertFalse(result.urgentReviewMayBeNeeded)
        assertEquals(null, result.guidanceChunk)
    }

    @Test
    fun lookupCopyAvoidsDiagnosisTreatmentDoseAndRiskScoreLanguage() {
        val builder = sourceFile("ui/UrgentProtocolLookup.kt").readText()
        val screen = sourceFile("ui/UrgentProtocolLookupScreen.kt").readText()
        val combined = "$builder\n$screen"

        assertTrue(combined.contains("Urgent protocol lookup"))
        assertTrue(combined.contains("Check urgent guidance"))
        assertTrue(combined.contains("Local guidance checked"))
        assertTrue(combined.contains("Health guidance used"))
        assertTrue(combined.contains("Urgent review may be needed"))
        assertTrue(combined.contains("This is not a diagnosis"))
        assertTrue(combined.contains("No visit, referral flag, or follow-up task is saved from this lookup."))
        listOf(
            "diagnosed",
            "treatment",
            "dosage",
            "dose",
            "risk score",
            "AI triage",
            "Emergency AI",
            "life-saving recommendation"
        ).forEach { forbidden ->
            assertFalse("Found forbidden urgent lookup wording: $forbidden", combined.contains(forbidden, ignoreCase = true))
        }
    }

    private fun signs(vararg labels: String): List<UrgentProtocolSign> {
        return labels.map { label ->
            UrgentProtocolLookupSigns.all.first { it.label == label }
        }
    }

    private fun assetCorpusJson(): String {
        val modulePath = File("src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        val rootPath = File("app/src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        return when {
            modulePath.exists() -> modulePath.readText()
            else -> rootPath.readText()
        }
    }

    private fun sourceFile(relativePath: String): File {
        val modulePath = File("src/main/java/com/smriti/clinicalscribe/$relativePath")
        val rootPath = File("app/src/main/java/com/smriti/clinicalscribe/$relativePath")
        return when {
            modulePath.exists() -> modulePath
            else -> rootPath
        }
    }

    private fun countryRankingJson(): String = """
        [
          {
            "id": "global_match",
            "region": "GLOBAL_CORE",
            "countryCode": null,
            "topic": "Global urgent marker",
            "keywords": ["shared urgent marker"],
            "citation": "WHO ANC Maternal danger signs",
            "referralLevel": "SAME_DAY",
            "text": "Global fallback guidance."
          },
          {
            "id": "region_match",
            "region": "INDIA",
            "countryCode": null,
            "topic": "Region urgent marker",
            "keywords": ["shared urgent marker"],
            "citation": "India regional CHW maternal danger signs",
            "referralLevel": "SAME_DAY",
            "text": "Region fallback guidance."
          },
          {
            "id": "exact_country",
            "region": "INDIA",
            "countryCode": "IN",
            "topic": "Exact country urgent marker",
            "keywords": ["shared urgent marker"],
            "citation": "India ASHA ANC danger signs",
            "referralLevel": "SAME_DAY",
            "text": "Country-specific guidance."
          }
        ]
    """.trimIndent()
}
