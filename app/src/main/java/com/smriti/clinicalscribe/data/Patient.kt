package com.smriti.clinicalscribe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val pregnancyWeeks: Int?,
    val village: String,
    val riskSummary: String
) {
    fun displayLabel(): String = "$name, ${age}${sex.firstOrNull() ?: ""}"
}
