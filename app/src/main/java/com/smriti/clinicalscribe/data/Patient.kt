package com.smriti.clinicalscribe.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smriti.clinicalscribe.rag.ProtocolRetrievalContext

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val pregnancyWeeks: Int?,
    val village: String,
    val riskSummary: String,
    val country: String = "India",
    val countryCode: String = "IN",
    val preferredLanguage: String = "en",
    val protocolRegion: String = "GLOBAL_CORE",
    val scenarioPreview: String = "",
    val notes: String? = null
) {
    fun displayLabel(): String = "$name, ${age}${sex.firstOrNull() ?: ""}"

    fun protocolContext(): ProtocolRetrievalContext {
        return ProtocolRetrievalContext(
            countryCode = countryCode.ifBlank { null },
            region = protocolRegion.ifBlank { null }
        )
    }

    fun protocolContextLabel(): String {
        val countryLabel = country.ifBlank { countryCode.ifBlank { "Local" } }
        val region = protocolRegion.uppercase().trim()
        return when {
            region == "GLOBAL_CORE" || region.isBlank() -> "Global guidance"
            else -> "$countryLabel guidance with global fallback"
        }
    }
}
