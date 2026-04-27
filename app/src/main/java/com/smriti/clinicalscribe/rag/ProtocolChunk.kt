package com.smriti.clinicalscribe.rag

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protocol_chunks")
data class ProtocolChunk(
    @PrimaryKey val id: String,
    val title: String,
    val source: String,
    val section: String,
    val text: String,
    val keywords: String
) {
    val citation: String
        get() = "$source $section"
}
