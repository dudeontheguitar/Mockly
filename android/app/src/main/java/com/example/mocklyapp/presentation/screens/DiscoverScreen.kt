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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.mocklyapp.domain.report.model.InterviewReport
import com.example.mocklyapp.domain.session.model.Session
import com.example.mocklyapp.presentation.theme.Poppins
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onInterviewClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSessionClick: (String) -> Unit,
    onResultClick: (String) -> Unit
) {
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
                Header(
                    userName = state.userName.ifBlank { "User" },
                    role = state.userRole
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                state.error != null -> {
                    item {
                        ErrorCard(
                            error = state.error ?: "Failed to load dashboard.",
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                else -> {
                    item {
                        MainDashboardCard(
                            openSlotsCount = state.openSlotsCount,
                            nextSession = state.nextSession,
                            lastReport = state.lastReport,
                            onInterviewClick = onInterviewClick
                        )
                    }

                    item { Spacer(modifier = Modifier.height(18.dp)) }

                    item {
                        NextInterviewCard(
                            session = state.nextSession,
                            onClick = {
                                state.nextSession?.id?.let(onSessionClick)
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(18.dp)) }

                    item {
                        LastResultCard(
                            session = state.lastEndedSession,
                            report = state.lastReport,
                            onClick = {
                                state.lastEndedSession?.id?.let(onResultClick)
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(18.dp)) }

                    item {
                        QuickActionsCard(
                            onInterviewClick = onInterviewClick,
                            onSettingsClick = onSettingsClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    userName: String,
    role: String
) {
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
                    text = "Welcome back,",
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

                if (role.isNotBlank()) {
                    Text(
                        text = role.uppercase(),
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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
private fun MainDashboardCard(
    openSlotsCount: Int,
    nextSession: Session?,
    lastReport: InterviewReport?,
    onInterviewClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF060446),
            Color(0xFF0A0932)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val overallScore = extractOverallScore(lastReport)

    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(22.dp)
        ) {
            Text(
                text = "Interview Dashboard",
                fontFamily = Poppins,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    nextSession != null -> "You have an upcoming interview."
                    openSlotsCount > 0 -> "There are open interview slots available."
                    else -> "No upcoming interviews yet."
                },
                fontFamily = Poppins,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = openSlotsCount.toString(),
                    label = "Open Slots"
                )

                StatItem(
                    value = if (nextSession != null) "1" else "0",
                    label = "Upcoming"
                )

                StatItem(
                    value = overallScore?.let { formatScore(it) } ?: "-",
                    label = "Last Score"
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onInterviewClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Open Interviews",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0A0932)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = Poppins,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = label,
            fontFamily = Poppins,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NextInterviewCard(
    session: Session?,
    onClick: () -> Unit
) {
    DashboardSection(
        title = "Next Interview"
    ) {
        if (session == null) {
            EmptyText("No upcoming interview. Book an available slot from the Interview tab.")
            return@DashboardSection
        }

        val title = session.interview?.title?.takeIf { it.isNotBlank() }
            ?: "Interview Session"

        val subtitle = buildString {
            val company = session.interview?.company.orEmpty()
            val location = session.interview?.location.orEmpty()

            if (company.isNotBlank()) append(company)
            if (company.isNotBlank() && location.isNotBlank()) append(" • ")
            if (location.isNotBlank()) append(location)

            if (isBlank()) append(session.status.name)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = formatSessionTime(session.startAt),
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LastResultCard(
    session: Session?,
    report: InterviewReport?,
    onClick: () -> Unit
) {
    DashboardSection(
        title = "Last Result"
    ) {
        if (session == null) {
            EmptyText("No completed interviews yet.")
            return@DashboardSection
        }

        if (report == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Text(
                    text = "Completed interview",
                    fontFamily = Poppins,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Report is not available yet.",
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            return@DashboardSection
        }

        val score = extractOverallScore(report)
        val label = report.metrics?.get("overallLabel")?.toString()
        val title = session.interview?.title?.takeIf { it.isNotBlank() }
            ?: "Completed interview"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = score?.let { formatScore(it) } ?: "-",
                    fontFamily = Poppins,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = "/ 100",
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (score != null) {
                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = (score / 100.0).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            }

            if (!label.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = label,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onInterviewClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    DashboardSection(
        title = "Quick Actions"
    ) {
        ActionRow(
            title = "Browse interview slots",
            description = "Find and book available interviews.",
            onClick = onInterviewClick
        )

        Spacer(Modifier.height(12.dp))

        ActionRow(
            title = "Edit your profile",
            description = "Update level, skills, bio and location.",
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.interview),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Text(
                text = description,
                fontFamily = Poppins,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Icon(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DashboardSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        fontFamily = Poppins,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.secondary
    )
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Failed to load dashboard",
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = error,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = Color.Red
            )

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onRetry) {
                Text("Retry", fontFamily = Poppins)
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
            else -> zoned.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }

        val timeLabel = zoned.toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))

        "$dateLabel, $timeLabel"
    } catch (_: Exception) {
        "-"
    }
}

private fun extractOverallScore(report: InterviewReport?): Double? {
    val value = report?.metrics?.get("overallScore")

    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

private fun formatScore(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}