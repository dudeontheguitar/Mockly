package com.example.mocklyapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionRole
import com.example.mocklyapp.domain.session.model.SessionStatus
import com.example.mocklyapp.models.OngoingInterviewItem
import com.example.mocklyapp.presentation.interview.InterviewViewModel
import com.example.mocklyapp.presentation.navigation.InterviewRegister
import com.example.mocklyapp.presentation.navigation.SessionDetailsRoute
import com.example.mocklyapp.presentation.theme.Poppins
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                val name = "${state.name}"
                Header(userName = name.ifBlank { "User" })
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                MyInterviews(
                    upcomingSessions = state.upcoming,
                    pastSessions = state.past,
                    onSessionClick = { sessionId ->
                        navController.navigate(SessionDetailsRoute(sessionId))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                AvailableInterview(navController = navController)
            }

            if (state.error != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.error!!,
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MyInterviews(
    upcomingSessions: List<Session>,
    pastSessions: List<Session>,
    onSessionClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past")

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // ... твой код заголовка и табов ...

        val currentList = if (selectedTab == 0) upcomingSessions else pastSessions

        if (currentList.isEmpty()) {
            Spacer(Modifier.height(12.dp))
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
                    onClick = { onSessionClick(session.id) }
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

    // имя интервьюера из участников
    val interviewerName = session.participants
        .firstOrNull { it.roleInSession == SessionRole.INTERVIEWER }
        ?.userDisplayName
        ?: "Interviewer"

    // красивое время справа
    val formattedTime = session.startAt?.let { iso ->
        try {
            val instant = Instant.parse(iso)
            val zoned = instant.atZone(ZoneId.systemDefault())

            val today = LocalDate.now()
            val dateLabel = when (zoned.toLocalDate()) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> zoned.toLocalDate().toString()
            }

            val timeLabel = zoned.toLocalTime()
                .format(DateTimeFormatter.ofPattern("h:mm a"))

            "$dateLabel, $timeLabel"
        } catch (_: Exception) {
            "-"
        }
    } ?: "-"

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
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

            Column {
                // ВЕРХНЯЯ СТРОКА — ИМЯ ИНТЕРВЬЮЕРА
                Text(
                    text = interviewerName,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                // НИЖНЯЯ СТРОКА — ПРОСТО ОПИСАНИЕ
                Text(
                    text = "Real mock interview",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            }

            Text(
                text = formattedTime,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun AvailableInterview(navController: NavHostController) {
    val cards = listOf(
        OngoingInterviewItem(
            title = "Frontend Developer",
            company = "Kaspi",
            location = "Remote",
            status = "Start",
            interviewerName = "Azamat S.",
            interviewerId = "1d17719b-90de-4b20-9460-a4eb26b834e1"
        ),
        OngoingInterviewItem(
            title = "Product Manager",
            company = "Tele2",
            location = "Remote",
            status = "Start",
            interviewerName = "Diana O.",
            interviewerId = "6818fe16-f988-4722-a529-85b4cfb1dfcf"
        ),
        OngoingInterviewItem(
            title = "QA Engineer",
            company = "Arbuz",
            location = "Remote",
            status = "Waiting",
            interviewerName = "Batyrkhan M.",
            interviewerId = "067b8ee7-1eb1-47c1-a75f-f93c01347465"
        )
    )

    var showDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedItem by remember { androidx.compose.runtime.mutableStateOf<OngoingInterviewItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Ongoing Interview",
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(Modifier.height(12.dp))

        cards.forEach { item ->
            OngoingCard(
                item = item,
                onClick = {
                    if (item.status == "Start") {
                        selectedItem = item
                        showDialog = true
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showDialog && selectedItem != null) {
        InterviewTypeDialog(
            onDismiss = { showDialog = false },
            onRealPerson = {
                val item = selectedItem!!
                showDialog = false
                navController.navigate(
                    InterviewRegister(
                        jobTitle = item.title,
                        company = item.company,
                        interviewerId = item.interviewerId,
                        interviewerName = item.interviewerName
                    )
                )
            },
            onAi = {
                // TODO: экран для AI интервью
                showDialog = false
            }
        )
    }
}

@Composable
private fun InterviewTypeDialog(
    onDismiss: () -> Unit,
    onRealPerson: () -> Unit,
    onAi: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Interview Type:",
                    fontFamily = Poppins,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(Modifier.height(24.dp))

                androidx.compose.material3.Button(
                    onClick = onRealPerson,
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0300A2)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3533B6))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.interview3),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Real Person",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                androidx.compose.material3.Button(
                    onClick = onAi,
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A0932)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B3A5B))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ai),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "With AI",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OngoingCard(item: OngoingInterviewItem, onClick: () -> Unit) {
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
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = item.title,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.location,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(999.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.status,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}
