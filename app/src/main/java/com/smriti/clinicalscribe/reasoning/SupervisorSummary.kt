package com.smriti.clinicalscribe.reasoning

data class SupervisorSummary(
    val totalVisits: Int,
    val referralsFlagged: Int,
    val urgentCases: List<String>,
    val followUpsDue: List<String>,
    val narrative: String
)
