package com.smriti.clinicalscribe.rag

import com.smriti.clinicalscribe.data.DemoSeedData
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolRetrieverTest {
    private val retriever = ProtocolRetriever.fromJson(assetCorpusJson())

    @Test
    fun protocolPackLoadsAtLeastFortyStructuredChunks() {
        assertTrue(retriever.allChunks().size >= 40)
    }

    @Test
    fun protocolPackContainsRequiredRegionTags() {
        val regions = retriever.allChunks().map { it.region }.toSet()

        assertTrue(regions.contains(ProtocolRegion.GLOBAL_CORE.name))
        assertTrue(regions.contains(ProtocolRegion.INDIA.name))
        assertTrue(regions.contains(ProtocolRegion.BANGLADESH.name))
        assertTrue(regions.contains(ProtocolRegion.ETHIOPIA.name))
        assertTrue(regions.contains(ProtocolRegion.AFRICA_REGION.name))
        assertTrue(regions.contains(ProtocolRegion.SOUTH_AMERICA_REGION.name))
    }

    @Test
    fun retrievesMaternalDangerSignProtocolForHeadacheBlurredVisionHighBpQuery() {
        val results = retriever.retrieve(
            query = "Pregnant mother has headache, blurred vision, and high BP",
            context = ProtocolRetrievalContext(countryCode = "IN", region = "INDIA")
        )

        assertTrue(results.isNotEmpty())
        assertEquals("mh_severe_headache", results.first().id)
        assertTrue(results.any { it.id == "mh_blurred_vision" })
        assertTrue(results.any { it.id == "mh_high_blood_pressure" })
        assertTrue(results.first().source == "Smriti Demo Maternal Health Protocol")
        assertTrue(results.first().section.startsWith("Danger Signs"))
        assertTrue(
            results.first().citation == "WHO ANC Recommendation B1.2" ||
                results.first().citation.startsWith("Smriti Demo Maternal Health Protocol")
        )
    }

    @Test
    fun returnsNoHallucinatedProtocolForUnrelatedQuery() {
        val results = retriever.retrieve("Broken water pump near the school needs repair")

        assertTrue(results.isEmpty())
    }

    @Test
    fun exactCountryMatchOutranksRegionAndGlobalMatches() {
        val localRetriever = ProtocolRetriever.fromJson(countryRankingJson())

        val results = localRetriever.retrieve(
            query = "shared referral marker",
            context = ProtocolRetrievalContext(countryCode = "IN", region = "AFRICA_REGION")
        )

        assertEquals("exact_country", results.first().id)
    }

    @Test
    fun regionMatchOutranksGlobalFallback() {
        val localRetriever = ProtocolRetriever.fromJson(countryRankingJson())

        val results = localRetriever.retrieve(
            query = "shared referral marker",
            context = ProtocolRetrievalContext(region = "AFRICA_REGION")
        )

        assertEquals("region_match", results.first().id)
    }

    @Test
    fun globalCoreFallbackStillRetrievesWhenCountrySpecificChunkIsAbsent() {
        val results = retriever.retrieve(
            query = "Mother reports maternal postpartum danger",
            context = ProtocolRetrievalContext(countryCode = "PE", region = "SOUTH_AMERICA_REGION")
        )

        assertTrue(results.isNotEmpty())
        assertEquals(ProtocolRegion.GLOBAL_CORE.name, results.first().region)
        assertEquals("global_core_postpartum_bleeding", results.first().id)
    }

    @Test
    fun luciaUsesSpanishPeruContextWithSouthAmericaFallbackNotBrazil() {
        val lucia = DemoSeedData.patients.first { it.id == "patient-lucia" }

        val results = retriever.retrieve(
            query = "South America high BP regional blood pressure concern with BP 150 over 95 and headache.",
            context = lucia.protocolContext()
        )

        assertEquals("Peru", lucia.country)
        assertEquals("PE", lucia.countryCode)
        assertEquals("es", lucia.preferredLanguage)
        assertTrue(lucia.country != "Brazil")
        assertTrue(results.isNotEmpty())
        assertEquals(ProtocolRegion.SOUTH_AMERICA_REGION.name, results.first().region)
    }

    @Test
    fun protocolRetrieverDoesNotIntroduceNetworkOrVectorDependency() {
        val source = File("src/main/java/com/smriti/clinicalscribe/rag/ProtocolRetriever.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/smriti/clinicalscribe/rag/ProtocolRetriever.kt")

        val text = source.readText()
        assertTrue(!text.contains("Http"))
        assertTrue(!text.contains("URL("))
        assertTrue(!text.contains("Socket"))
        assertTrue(!text.contains("vector", ignoreCase = true))
    }

    private fun assetCorpusJson(): String {
        val modulePath = File("src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        val rootPath = File("app/src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        return when {
            modulePath.exists() -> modulePath.readText()
            else -> rootPath.readText()
        }
    }

    private fun countryRankingJson(): String = """
        [
          {
            "id": "global_match",
            "region": "GLOBAL_CORE",
            "countryCode": null,
            "topic": "Global match",
            "keywords": ["shared referral marker"],
            "citation": "WHO ANC Maternal danger signs",
            "referralLevel": "SAME_DAY",
            "text": "Global fallback guidance.",
            "safetyNotes": "Fallback only."
          },
          {
            "id": "region_match",
            "region": "AFRICA_REGION",
            "countryCode": null,
            "topic": "Region match",
            "keywords": ["shared referral marker"],
            "citation": "Regional CHW maternal danger signs",
            "referralLevel": "SAME_DAY",
            "text": "Regional fallback guidance.",
            "safetyNotes": "Fallback only."
          },
          {
            "id": "exact_country",
            "region": "INDIA",
            "countryCode": "IN",
            "topic": "Exact country match",
            "keywords": ["shared referral marker"],
            "citation": "India ASHA ANC danger signs",
            "referralLevel": "SAME_DAY",
            "text": "Country-specific guidance.",
            "safetyNotes": "Country match."
          }
        ]
    """.trimIndent()
}
