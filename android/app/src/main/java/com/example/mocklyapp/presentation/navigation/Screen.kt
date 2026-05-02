package com.example.mocklyapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object DiscoverRoute

@Serializable
object InterviewRoute

@Serializable
object SettingsRoute

@Serializable
object Onboarding1

@Serializable
object Onboarding2

@Serializable
object Onboarding3

@Serializable
object Login

@Serializable
object Register

@Serializable
object EditProfileRoute

@Serializable
object ChangePasswordRoute

@Serializable
object RoleEntryRoute

@Serializable
object InterviewerRootRoute

@Serializable
object InterviewerSessionsRoute

@Serializable
object CreateInterviewSlotRoute

@Serializable
data class InterviewRegister(
    val slotId: String,
    val title: String,
    val company: String,
    val location: String,
    val interviewerName: String,
    val scheduledAt: String?,
    val durationMinutes: Int
)

@Serializable
data class SessionDetailsRoute(
    val sessionId: String
)

@Serializable
data class MockInterviewRoute(
    val sessionId: String
)

@Serializable
data class InterviewResultsRoute(
    val sessionId: String,
    val noReportReason: String? = null
)