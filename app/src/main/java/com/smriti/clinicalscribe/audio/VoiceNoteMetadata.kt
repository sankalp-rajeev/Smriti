package com.smriti.clinicalscribe.audio

data class VoiceNoteMetadata(
    val audioFilePath: String,
    val audioDurationSeconds: Int
) {
    val fileName: String
        get() = audioFilePath.substringAfterLast('/')
}
