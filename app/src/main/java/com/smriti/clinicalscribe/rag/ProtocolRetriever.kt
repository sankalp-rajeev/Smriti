package com.smriti.clinicalscribe.rag

import android.content.Context

class ProtocolRetriever(
    private val chunks: List<ProtocolChunk>
) {
    fun retrieve(
        query: String,
        context: ProtocolRetrievalContext? = null
    ): List<ProtocolChunk> {
        val normalizedQuery = query.lowercase()
        if (normalizedQuery.isBlank()) return emptyList()

        return chunks
            .map { chunk ->
                ProtocolMatch(
                    chunk = chunk,
                    keywordScore = score(chunk, normalizedQuery),
                    locationScore = locationScore(chunk, context)
                )
            }
            .filter { match -> match.keywordScore > 0 }
            .sortedWith(
                compareByDescending<ProtocolMatch> { it.locationScore }
                    .thenByDescending { it.keywordScore }
                    .thenBy { it.chunk.id }
            )
            .map { match -> match.chunk }
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

    private fun locationScore(
        chunk: ProtocolChunk,
        context: ProtocolRetrievalContext?
    ): Int {
        if (context == null) return 0

        val requestedCountry = context.countryCode?.uppercase()
        val requestedRegion = context.region?.uppercase()
        val chunkCountry = chunk.countryCode?.uppercase()
        val chunkRegion = chunk.region.uppercase()

        return when {
            requestedCountry != null && chunkCountry == requestedCountry -> 30
            requestedRegion != null && chunkRegion == requestedRegion -> 20
            chunkRegion == ProtocolRegion.GLOBAL_CORE.name -> 10
            else -> 0
        }
    }

    private data class ProtocolMatch(
        val chunk: ProtocolChunk,
        val keywordScore: Int,
        val locationScore: Int
    )

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
                    title = stringField(objectText, "title").ifBlank { stringField(objectText, "topic") },
                    source = stringField(objectText, "source_name")
                        .ifBlank { sourceFromCitation(stringField(objectText, "citation")) },
                    section = stringField(objectText, "source_section")
                        .ifBlank { sectionFromCitation(stringField(objectText, "citation")) },
                    text = stringField(objectText, "guidance_text").ifBlank { stringField(objectText, "text") },
                    keywords = arrayField(objectText, "keywords").joinToString("|"),
                    referralLevel = stringField(objectText, "referral_level")
                        .ifBlank { stringField(objectText, "referralLevel") }
                        .ifBlank { "UNSPECIFIED" },
                    region = stringField(objectText, "region").ifBlank { ProtocolRegion.GLOBAL_CORE.name },
                    countryCode = nullableStringField(objectText, "countryCode")?.uppercase(),
                    topic = stringField(objectText, "topic").ifBlank { stringField(objectText, "title") },
                    safetyNotes = nullableStringField(objectText, "safetyNotes"),
                    citationText = stringField(objectText, "citation")
                )
            }.toList()
        }

        private fun nullableStringField(objectText: String, fieldName: String): String? {
            val nullRegex = Regex("\"$fieldName\"\\s*:\\s*null")
            if (nullRegex.containsMatchIn(objectText)) return null
            return stringField(objectText, fieldName).ifBlank { null }
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

        private fun sourceFromCitation(citation: String): String {
            return citation.substringBefore(" - ").ifBlank { citation }
        }

        private fun sectionFromCitation(citation: String): String {
            return citation.substringAfter(" - ", missingDelimiterValue = "").ifBlank { "Protocol" }
        }
    }
}

data class ProtocolRetrievalContext(
    val countryCode: String? = null,
    val region: String? = null
)

enum class ProtocolRegion {
    GLOBAL_CORE,
    INDIA,
    BANGLADESH,
    ETHIOPIA,
    AFRICA_REGION,
    SOUTH_AMERICA_REGION
}
