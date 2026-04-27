package com.smriti.clinicalscribe.rag

import android.content.Context

class ProtocolRetriever(
    private val chunks: List<ProtocolChunk>
) {
    fun retrieve(query: String): List<ProtocolChunk> {
        val normalizedQuery = query.lowercase()
        if (normalizedQuery.isBlank()) return emptyList()

        return chunks
            .map { chunk -> chunk to score(chunk, normalizedQuery) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (chunk, _) -> chunk }
    }

    fun allChunks(): List<ProtocolChunk> = chunks

    private fun score(chunk: ProtocolChunk, normalizedQuery: String): Int {
        return chunk.keywordList()
            .count { keyword -> keyword.isNotBlank() && normalizedQuery.contains(keyword) }
    }

    private fun ProtocolChunk.keywordList(): List<String> {
        return keywords
            .split("|")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
    }

    companion object {
        const val ASSET_PATH = "protocols/maternal_health_demo_protocols.json"

        fun fromAsset(context: Context): ProtocolRetriever {
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            return fromJson(json)
        }

        fun fromJson(json: String): ProtocolRetriever {
            return ProtocolRetriever(parseChunks(json))
        }

        private fun parseChunks(json: String): List<ProtocolChunk> {
            val objectRegex = Regex("\\{(.*?)\\}", setOf(RegexOption.DOT_MATCHES_ALL))
            return objectRegex.findAll(json).map { objectMatch ->
                val objectText = objectMatch.groupValues[1]
                ProtocolChunk(
                    id = stringField(objectText, "id"),
                    title = stringField(objectText, "title"),
                    source = stringField(objectText, "source_name"),
                    section = stringField(objectText, "source_section"),
                    text = stringField(objectText, "guidance_text"),
                    keywords = arrayField(objectText, "keywords").joinToString("|"),
                    referralLevel = stringField(objectText, "referral_level")
                )
            }.toList()
        }

        private fun stringField(objectText: String, fieldName: String): String {
            val regex = Regex("\"$fieldName\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
            return regex.find(objectText)?.groupValues?.get(1)?.decodeJsonString().orEmpty()
        }

        private fun arrayField(objectText: String, fieldName: String): List<String> {
            val arrayRegex = Regex("\"$fieldName\"\\s*:\\s*\\[(.*?)\\]", setOf(RegexOption.DOT_MATCHES_ALL))
            val arrayText = arrayRegex.find(objectText)?.groupValues?.get(1).orEmpty()
            val stringRegex = Regex("\"((?:\\\\.|[^\"])*)\"")
            return stringRegex.findAll(arrayText)
                .map { it.groupValues[1].decodeJsonString() }
                .toList()
        }

        private fun String.decodeJsonString(): String {
            return replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
        }
    }
}
