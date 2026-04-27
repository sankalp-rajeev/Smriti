package com.smriti.clinicalscribe.rag

import com.smriti.clinicalscribe.data.DemoSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolRetrieverTest {
    private val retriever = ProtocolRetriever(DemoSeedData.protocolChunks)

    @Test
    fun retrievesMaternalDangerSignProtocolForHeadacheHighBpQuery() {
        val results = retriever.retrieve("Pregnant mother has headache, blurred vision, and high BP")

        assertTrue(results.isNotEmpty())
        assertEquals("anc-danger-signs", results.first().id)
        assertTrue(results.first().citation.contains("WHO ANC Recommendation B1.2"))
    }

    @Test
    fun returnsNoHallucinatedProtocolForUnrelatedQuery() {
        val results = retriever.retrieve("Broken water pump near the school needs repair")

        assertTrue(results.isEmpty())
    }
}
