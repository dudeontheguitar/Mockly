package com.example.mocklyapp.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.interview.InterviewRegisterViewModel
import com.example.mocklyapp.presentation.theme.Poppins
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InterviewRegisterScreen(
    viewModel: InterviewRegisterViewModel,
    onBack: () -> Unit,
    onSuccessOK: (sessionId: String) -> Unit,
    jobTitle: String,
    company: String,
    location: String,
    interviewerName: String,
    scheduledAt: String?,
    durationMinutes: Int
) {
    val state by viewModel.state.collectAsState()

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onBack() }
                        .size(30.dp)
                )

                Text(
                    text = "Interview Details",
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.alem),
                            contentDescription = null,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        Column {
                            Text(
                                text = jobTitle,
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = company.ifBlank { "Company not specified" },
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFE5E5E5),
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    InfoLine(
                        iconRes = R.drawable.interview4,
                        label = "Interviewer",
                        value = interviewerName
                    )

                    Spacer(Modifier.height(14.dp))

                    InfoLine(
                        iconRes = R.drawable.duration,
                        label = "Duration",
                        value = "$durationMinutes min"
                    )

                    Spacer(Modifier.height(14.dp))

                    InfoLine(
                        iconRes = R.drawable.duration,
                        label = "Time",
                        value = formatSlotTime(scheduledAt)
                    )

                    if (location.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))

                        InfoLine(
                            iconRes = R.drawable.profile,
                            label = "Location",
                            value = location
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Before you join",
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Please make sure your camera and microphone are available. The interview audio will be recorded for AI report generation.",
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = state.isAgree,
                    onCheckedChange = { viewModel.setAgree(it) },
                    modifier = Modifier.size(30.dp)
                )

                Text(
                    text = "I agree to the interview guidelines",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.weight(1f))

            state.error?.let { msg ->
                Text(
                    text = msg,
                    color = Color.Red,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(65.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isAgree) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0x990A0932)
                    }
                ),
                onClick = {
                    viewModel.register()
                },
                enabled = state.isAgree && !state.isLoading
            ) {
                Text(
                    text = if (state.isLoading) "Please wait..." else "Book Interview",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )
            }

            if (state.isSuccess) {
                RegisterSuccessDialog(
                    onDismiss = { },
                    onOk = {
                        val sessionId = state.createdSessionId
                        if (!sessionId.isNullOrBlank()) {
                            onSuccessOK(sessionId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoLine(
    iconRes: Int,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "$label: ",
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Text(
            text = value.ifBlank { "-" },
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun RegisterSuccessDialog(
    onDismiss: () -> Unit,
    onOk: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(70.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Success!",
                    fontFamily = Poppins,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "You have booked the interview successfully.",
                    textAlign = TextAlign.Center,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onOk,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "OK",
                        fontFamily = Poppins,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
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