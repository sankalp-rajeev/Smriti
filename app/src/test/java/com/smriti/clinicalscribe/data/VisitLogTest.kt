package com.smriti.clinicalscribe.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VisitLogTest {
    @Test
    fun supportsOptionalAudioMetadata() {
        val visitLog = VisitLog(
            patientId = "patient-meena",
            visitDateMillis = 1_700_000_000_000L,
            observationText = "Simulated transcript",
            structuredNote = "Observation:\nSimulated transcript",
            protocolCitation = "Smriti Demo Maternal Health Protocol Danger Signs 1.1",
            suggestedFollowUp = "Follow up with supervisor.",
            confirmed = true,
            audioFilePath = "/data/user/0/com.smriti.clinicalscribe/files/voice_notes/voice_note.m4a",
            audioDurationSeconds = 24,
            transcriptSource = TranscriptSource.REAL_ASR_PENDING
        )

        assertEquals("/data/user/0/com.smriti.clinicalscribe/files/voice_notes/voice_note.m4a", visitLog.audioFilePath)
        assertEquals(24, visitLog.audioDurationSeconds)
        assertEquals(TranscriptSource.REAL_ASR_PENDING, visitLog.transcriptSource)
    }
}
