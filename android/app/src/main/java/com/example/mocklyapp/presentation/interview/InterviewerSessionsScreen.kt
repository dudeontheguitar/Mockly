package com.example.mocklyapp.presentation.interview

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.domain.interviewslot.model.InterviewSlot
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionRole
import com.example.mocklyapp.presentation.theme.Poppins
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InterviewerSessionsScreen(
    viewModel: InterviewViewModel,
    onSessionClick: (String) -> Unit = {},
    onPastSessionClick: (String) -> Unit,
    onCreateSlotClick: () -> Unit
){
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InterviewerHeader(
                    userName = state.name.ifBlank { "Interviewer" }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Button(
                    onClick = onCreateSlotClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "+ Create Interview Slot",
                        fontFamily = Poppins,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                InterviewerOpenSlots(
                    isLoading = state.isMySlotsLoading,
                    slots = state.mySlots,
                    error = state.mySlotsError
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (state.isLoading) {
                item {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                item {
                    InterviewerMyInterviews(
                        upcomingSessions = state.upcoming,
                        pastSessions = state.past,
                        onSessionClick = onSessionClick,
                        onPastSessionClick = onPastSessionClick
                    )
                }
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
private fun InterviewerHeader(userName: String) {
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
private fun InterviewerOpenSlots(
    isLoading: Boolean,
    slots: List<InterviewSlot>,
    error: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "My Open Slots",
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
                    fontFamily = Poppins,
                    fontSize = 14.sp
                )
            }

            slots.isEmpty() -> {
                Text(
                    text = "No open slots yet. Create one so candidates can book it.",
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = Poppins,
                    fontSize = 14.sp
                )
            }

            else -> {
                slots.forEach { slot ->
                    InterviewSlotCard(slot)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun InterviewSlotCard(slot: InterviewSlot) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = slot.title,
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = slot.company.ifBlank { "Company not specified" },
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = formatSessionTime(slot.scheduledAt),
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            if (slot.location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = slot.location,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun InterviewerMyInterviews(
    upcomingSessions: List<Session>,
    pastSessions: List<Session>,
    onSessionClick: (String) -> Unit,
    onPastSessionClick: (String) -> Unit
) {
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

        val currentList = if (selectedTab == 0) {
            upcomingSessions
        } else {
            pastSessions
        }

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
                InterviewerSessionCard(
                    session = session,
                    onClick = {
                        if (selectedTab == 0) {
                            onSessionClick(session.id)
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
private fun InterviewerSessionCard(
    session: Session,
    onClick: () -> Unit
) {
    val candidateName = session.participants
        .firstOrNull { it.roleInSession == SessionRole.CANDIDATE }
        ?.userDisplayName
        ?: "Candidate"

    val title = session.interview?.title
        ?.takeIf { it.isNotBlank() }
        ?: "Interview Session"

    val formattedTime = formatSessionTime(session.startAt)

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
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
                    text = candidateName,
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

private fun formatSessionTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"

    return try {
        val instant = Instant.parse(iso)
        val zoned = instant.atZone(ZoneId.systemDefault())

        val today = LocalDate.now()
        val dateLabel = when (zoned.toLocalDate()) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> zoned.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }

        val timeLabel = zoned.toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))

        "$dateLabel, $timeLabel"
    } catch (_: Exception) {
        "-"
    }
}