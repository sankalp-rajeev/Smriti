package com.smriti.clinicalscribe.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visit_logs",
    indices = [Index(value = ["patientId"])]
)
data class VisitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: String,
    val visitDateMillis: Long,
    val observationText: String,
    val structuredNote: String,
    val protocolCitation: String,
    val suggestedFollowUp: String,
    val confirmed: Boolean
)
