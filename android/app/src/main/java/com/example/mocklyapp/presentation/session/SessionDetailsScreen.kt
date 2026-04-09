package com.example.mocklyapp.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

            /** ------------------ TOP BAR ------------------ */
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
                        fontSize = 24.sp // ИЗМЕНЕНО +4sp
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            /** ------------------ MAIN INFO CARD ------------------ */
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(0.dp) // УБРАНЫ ТЕНИ
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
                                text = state.title.ifBlank { "Product Manager" },
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp // ИЗМЕНЕНО +4sp
                                ),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                            Text(
                                text = state.company.ifBlank { "Kaspi" },
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 16.sp, // ИЗМЕНЕНО +2sp
                                    fontWeight = FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
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
                        value = state.interviewerName
                    )
                    Spacer(Modifier.height(10.dp))

                    InfoRow(
                        iconRes = R.drawable.duration,
                        label = "Duration",
                        value = "30 min"
                    )
                    Spacer(Modifier.height(10.dp))

                    InfoRow(
                        iconRes = R.drawable.profile,
                        label = "Date",
                        value = state.formattedTime
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            /** ------------------ DEVICE CHECK CARD ------------------ */
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(0.dp) // УБРАНО
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Device Check",
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp // +4sp
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )

                    Spacer(Modifier.height(16.dp))

                    DeviceRow("Camera", "Ready")
                    DeviceRow("Microphone", "Ready")
                    DeviceRow("Connection", "Good")
                }
            }

            Spacer(Modifier.height(20.dp))

            /** ------------------ NOTE CARD ------------------ */
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
                        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), 0, 5)
                        append("Make sure you're in a quiet environment and have good lighting for the best interview experience.")
                    },
                    modifier = Modifier.padding(18.dp),
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 16.sp, // ИЗМЕНЕНО +2sp
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.weight(1f))

            /** ------------------ START BUTTON ------------------ */
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp), // УВЕЛИЧЕНО
                onClick = onStartClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Start",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 20.sp, // +2sp
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun InfoRow(iconRes: Int, label: String, value: String) {
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
                addStyle(SpanStyle(fontWeight = FontWeight.Normal), 0, label.length + 2)
                append(value)
                addStyle(
                    SpanStyle(fontWeight = FontWeight.SemiBold),
                    label.length + 2,
                    label.length + 2 + value.length
                )
            },
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp // +2sp
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun DeviceRow(label: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp, // +2sp
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF26A65B))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = status,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 14.sp, // +2sp
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        }
    }
}
