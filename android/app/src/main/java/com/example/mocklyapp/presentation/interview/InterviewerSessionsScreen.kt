package com.example.mocklyapp.presentation.interview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.mocklyapp.R
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.domain.session.model.SessionRole
import com.example.mocklyapp.presentation.theme.Poppins
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InterviewerSessionsScreen(
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
                val name = state.name.ifBlank { "User" }
                InterviewerHeader(userName = name)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                InterviewerMyInterviews(
                    upcomingSessions = state.upcoming,
                    pastSessions = state.past
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                CreateInterviewSection()
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

/* ------------------------------ HEADER ------------------------------------ */

@Composable
private fun InterviewerHeader(userName: String) {
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

/* --------------------------- MY INTERVIEWS -------------------------------- */

@Composable
private fun InterviewerMyInterviews(
    upcomingSessions: List<Session>,
    pastSessions: List<Session>
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past")

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                    Modifier
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
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (selectedTab == index)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

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
                InterviewerSessionCard(session)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InterviewerSessionCard(session: Session) {

    val candidateName = session.participants
        .firstOrNull { it.roleInSession == SessionRole.CANDIDATE }
        ?.userDisplayName
        ?: "Candidate"

    val candidateLevel = "Junior" // пока мок

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
        modifier = Modifier.fillMaxWidth(),
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

            Column {
                Text(
                    text = candidateName,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = candidateLevel,
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

/* --------------------------- CREATE INTERVIEW ----------------------------- */

data class CreateTemplateItem(
    val title: String,
    val location: String
)

@Composable
private fun CreateInterviewSection() {
    val items = listOf(
        CreateTemplateItem("Frontend Developer", "Remote"),
        CreateTemplateItem("Product Manager", "Remote"),
        CreateTemplateItem("ML Engineer", "Remote")
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Create Interview",
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(Modifier.height(12.dp))

        items.forEach { item ->
            CreateInterviewCard(item = item, onClick = {
                // TODO: потом тут откроешь экран создания / выбора кандидата
            })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CreateInterviewCard(
    item: CreateTemplateItem,
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
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 24.dp, vertical = 18.dp)
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
                        text = "Create",
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
