package com.smriti.clinicalscribe.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "referral_flags",
    indices = [Index(value = ["patientId"]), Index(value = ["visitLogId"])]
)
data class ReferralFlag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitLogId: Long? = null,
    val patientId: String,
    val urgency: String,
    val reason: String,
    val protocolBasis: String,
    val recommendedFacility: String,
    val dangerSigns: String,
    val createdAtMillis: Long
)
