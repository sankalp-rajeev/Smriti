package com.smriti.clinicalscribe.reasoning

data class SupervisorSummary(
    val totalVisits: Int,
    val referralsFlagged: Int,
    val urgentCases: List<String>,
    val followUpsDue: List<String>,
    val narrative: String,
    val paperScanNeedsUrgentReview: List<String> = emptyList(),
    val openFollowUps: Int = 0,
    val overdueFollowUps: Int = 0,
    val dueTodayFollowUps: Int = 0,
    val upcomingFollowUps: Int = 0
)
