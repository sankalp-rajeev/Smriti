package com.smriti.clinicalscribe.rag

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolRetrieverTest {
    private val retriever = ProtocolRetriever.fromJson(assetCorpusJson())

    @Test
    fun retrievesMaternalDangerSignProtocolForHeadacheBlurredVisionHighBpQuery() {
        val results = retriever.retrieve("Pregnant mother has headache, blurred vision, and high BP")

        assertTrue(results.isNotEmpty())
        assertEquals("mh_severe_headache", results.first().id)
        assertTrue(results.any { it.id == "mh_blurred_vision" })
        assertTrue(results.any { it.id == "mh_high_blood_pressure" })
        assertTrue(results.first().source == "Smriti Demo Maternal Health Protocol")
        assertTrue(results.first().section.startsWith("Danger Signs"))
    }

    @Test
    fun returnsNoHallucinatedProtocolForUnrelatedQuery() {
        val results = retriever.retrieve("Broken water pump near the school needs repair")

        assertTrue(results.isEmpty())
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
