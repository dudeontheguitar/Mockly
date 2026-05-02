package com.example.mocklyapp.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.sessiondetails.SessionDetailsViewModel
import com.example.mocklyapp.presentation.theme.Poppins

@Composable
fun SessionDetailsScreen(
    viewModel: SessionDetailsViewModel,
    onBack: () -> Unit,
    onStartClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            TopBar(onBack = onBack)

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load session.",
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = state.error ?: "",
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 14.sp
                                ),
                                color = Color.Red
                            )

                            Spacer(Modifier.height(12.dp))

                            TextButton(onClick = { viewModel.load() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    SessionDetailsContent(
                        title = state.title,
                        company = state.company,
                        location = state.location,
                        description = state.description,
                        durationMinutes = state.durationMinutes,
                        interviewerName = state.interviewerName,
                        candidateName = state.candidateName,
                        formattedTime = state.formattedTime,
                        statusText = state.statusText,
                        canJoin = state.canJoin,
                        onStartClick = onStartClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.arrow_back),
            contentDescription = "Back",
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.CenterStart)
                .clickable { onBack() },
            tint = MaterialTheme.colorScheme.primaryContainer
        )

        Text(
            text = "Interview Details",
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun SessionDetailsContent(
    title: String,
    company: String,
    location: String,
    description: String,
    durationMinutes: Int,
    interviewerName: String,
    candidateName: String,
    formattedTime: String,
    statusText: String,
    canJoin: Boolean,
    onStartClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8E8E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = title.ifBlank { "Interview Session" },
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )

                    if (company.isNotBlank()) {
                        Text(
                            text = company,
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE6E6E6))
            )

            Spacer(Modifier.height(14.dp))

            InfoRow(
                iconRes = R.drawable.profile,
                label = "Interviewer",
                value = interviewerName
            )

            Spacer(Modifier.height(10.dp))

            InfoRow(
                iconRes = R.drawable.profile,
                label = "Candidate",
                value = candidateName
            )

            Spacer(Modifier.height(10.dp))

            InfoRow(
                iconRes = R.drawable.duration,
                label = "Duration",
                value = "$durationMinutes min"
            )

            Spacer(Modifier.height(10.dp))

            InfoRow(
                iconRes = R.drawable.profile,
                label = "Date",
                value = formattedTime
            )

            if (location.isNotBlank()) {
                Spacer(Modifier.height(10.dp))

                InfoRow(
                    iconRes = R.drawable.profile,
                    label = "Location",
                    value = location
                )
            }

            Spacer(Modifier.height(10.dp))

            InfoRow(
                iconRes = R.drawable.profile,
                label = "Status",
                value = statusText
            )

            if (description.isNotBlank()) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = description,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFF4FF)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append("Note: ")
                addStyle(
                    SpanStyle(fontWeight = FontWeight.SemiBold),
                    0,
                    5
                )
                append("Make sure you're in a quiet environment. Camera and microphone permissions will be requested when you join.")
            },
            modifier = Modifier.padding(18.dp),
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }

    Spacer(Modifier.height(24.dp))

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        enabled = canJoin,
        onClick = onStartClick,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color(0x990A0932)
        )
    ) {
        Text(
            text = if (canJoin) "Join Interview" else "Interview Ended",
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}

@Composable
private fun InfoRow(
    iconRes: Int,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = buildAnnotatedString {
                append("$label: ")
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Normal),
                    0,
                    label.length + 2
                )
                append(value.ifBlank { "-" })
                addStyle(
                    SpanStyle(fontWeight = FontWeight.SemiBold),
                    label.length + 2,
                    label.length + 2 + value.ifBlank { "-" }.length
                )
            },
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}