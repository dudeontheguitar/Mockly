package com.example.mocklyapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mocklyapp.R
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlot
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionRole
import com.example.mocklyapp.presentation.interview.InterviewViewModel
import com.example.mocklyapp.presentation.navigation.InterviewRegister
import com.example.mocklyapp.presentation.navigation.SessionDetailsRoute
import com.example.mocklyapp.presentation.theme.Poppins
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.mocklyapp.presentation.navigation.InterviewResultsRoute

@Composable
fun InterviewScreen(
    navController: NavHostController,
    viewModel: InterviewViewModel
) {
    val state by viewModel.state.collectAsState()

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Header(userName = state.name.ifBlank { "User" })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                MyInterviews(
                    isLoading = state.isLoading,
                    upcomingSessions = state.upcoming,
                    pastSessions = state.past,
                    onUpcomingSessionClick = { sessionId ->
                        navController.navigate(SessionDetailsRoute(sessionId))
                    },
                    onPastSessionClick = { sessionId ->
                        navController.navigate(InterviewResultsRoute(sessionId))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                AvailableInterviewSlots(
                    isLoading = state.isSlotsLoading,
                    slots = state.availableSlots,
                    error = state.slotsError,
                    onSlotClick = { slot ->
                        navController.navigate(
                            InterviewRegister(
                                slotId = slot.id,
                                title = slot.title,
                                company = slot.company,
                                location = slot.location,
                                interviewerName = slot.interviewer?.displayName ?: "Interviewer",
                                scheduledAt = slot.scheduledAt,
                                durationMinutes = slot.durationMinutes
                            )
                        )
                    }
                )
            }

            if (state.error != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.error ?: "",
                        color = Color.Red,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8E8E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Let's work,",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Text(
                    text = userName,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_notifications_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MyInterviews(
    isLoading: Boolean,
    upcomingSessions: List<Session>,
    pastSessions: List<Session>,
    onUpcomingSessionClick: (String) -> Unit,
    onPastSessionClick: (String) -> Unit
){
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "My Interviews",
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(Modifier.height(12.dp))

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primaryContainer,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedTab)
                        .padding(horizontal = 48.dp)
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == index) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            }
                        ),
                        color = if (selectedTab == index) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        val currentList = if (selectedTab == 0) upcomingSessions else pastSessions

        if (currentList.isEmpty()) {
            Text(
                text = "No interviews yet",
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            currentList.forEach { session ->
                SessionCard(
                    session = session,
                    onClick = {
                        if (selectedTab == 0) {
                            onUpcomingSessionClick(session.id)
                        } else {
                            onPastSessionClick(session.id)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit
) {
    val interviewerName = session.participants
        .firstOrNull { it.roleInSession == SessionRole.INTERVIEWER }
        ?.userDisplayName
        ?: "Interviewer"

    val title = session.interview?.title
        ?.takeIf { it.isNotBlank() }
        ?: "Interview Session"

    val subtitle = buildString {
        val company = session.interview?.company.orEmpty()
        val location = session.interview?.location.orEmpty()

        if (company.isNotBlank()) append(company)
        if (company.isNotBlank() && location.isNotBlank()) append(" • ")
        if (location.isNotBlank()) append(location)

        if (isBlank()) append(interviewerName)
    }

    val formattedTime = formatSessionTime(session.startAt)

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = formattedTime,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun AvailableInterviewSlots(
    isLoading: Boolean,
    slots: List<InterviewSlot>,
    error: String?,
    onSlotClick: (InterviewSlot) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Available Interviews",
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            error != null -> {
                Text(
                    text = error,
                    color = Color.Red,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 14.sp
                    )
                )
            }

            slots.isEmpty() -> {
                Text(
                    text = "No open interview slots available.",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            else -> {
                slots.forEach { slot ->
                    InterviewSlotCard(
                        slot = slot,
                        onClick = { onSlotClick(slot) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun InterviewSlotCard(
    slot: InterviewSlot,
    onClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF060446),
            Color(0xFF0A0932)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slot.title,
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = slot.company.ifBlank { "Company not specified" },
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = slot.interviewer?.displayName ?: "Interviewer",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(999.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Book",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = formatSlotTime(slot.scheduledAt),
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.85f)
                )

                if (slot.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = slot.location,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 13.sp
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatSessionTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"

    return try {
        val instant = Instant.parse(iso)
        val zoned = instant.atZone(ZoneId.systemDefault())

        val today = LocalDate.now()
        val dateLabel = when (zoned.toLocalDate()) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> zoned.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM"))
        }

        val timeLabel = zoned.toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))

        "$dateLabel, $timeLabel"
    } catch (_: Exception) {
        "-"
    }
}

private fun formatSlotTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "Time not specified"

    return try {
        val instant = Instant.parse(iso)
        val zoned = instant.atZone(ZoneId.systemDefault())

        val date = zoned.toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val time = zoned.toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))

        "$date, $time"
    } catch (_: Exception) {
        "Time not specified"
    }
}