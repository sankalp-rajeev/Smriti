package com.smriti.clinicalscribe.rag

import com.smriti.clinicalscribe.data.DemoSeedData

class ProtocolRetriever(
    private val chunks: List<ProtocolChunk> = DemoSeedData.protocolChunks
) {
    fun retrieve(query: String): List<ProtocolChunk> {
        val normalizedQuery = query.lowercase()
        return chunks
            .map { chunk -> chunk to score(chunk, normalizedQuery) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (chunk, _) -> chunk }
            .ifEmpty { chunks.take(1) }
    }

    private fun score(chunk: ProtocolChunk, normalizedQuery: String): Int {
        return chunk.keywords
            .split(" ")
            .count { keyword -> keyword.isNotBlank() && normalizedQuery.contains(keyword) }
    }
}
