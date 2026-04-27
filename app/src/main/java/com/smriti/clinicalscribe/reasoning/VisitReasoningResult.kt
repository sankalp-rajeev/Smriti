package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.rag.ProtocolChunk

data class VisitReasoningResult(
    val patientId: String,
    val observationText: String,
    val structuredNote: String,
    val referralFlag: ReferralFlag?,
    val protocolCitation: String,
    val suggestedFollowUp: String,
    val protocolChunk: ProtocolChunk?,
    val uncertain: Boolean,
    val clarificationPrompt: String?
)
