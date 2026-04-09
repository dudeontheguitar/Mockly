package com.example.mocklyapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object DiscoverRoute

@Serializable
object InterviewRoute

@Serializable
object MessageRoute

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
data class InterviewRegister(
    val jobTitle: String,
    val company: String,
    val interviewerId: String,
    val interviewerName: String
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
    val sessionId: String
)

