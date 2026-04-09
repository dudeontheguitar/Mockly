package com.example.mocklyapp.presentation.interview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.theme.Poppins

@Composable
fun InterviewResultsScreen(
    onBack: () -> Unit
) {
    // Рандомный скор от 80 до 98 (фиксируется при первом появлении)
    val score by remember { mutableStateOf((80..98).random()) }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onBackground)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Interview Results",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Score Circle
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0C087A),
                                    Color(0xFF0300A2)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score%",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Score",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Overall Message (захардкожено как в макете)
                Text(
                    text = "Great Job!",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You've performed well in this interview",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // Strengths (примерные пункты)
                FeedbackCard(
                    title = "Strengths",
                    items = listOf(
                        "Clear communication and articulation",
                        "Strong technical knowledge in React",
                        "Good problem-solving approach",
                        "Confident body language"
                    ),
                    iconColor = Color(0xFF34C759),
                    iconRes = R.drawable.duration
                )

                Spacer(Modifier.height(16.dp))

                // Areas to Improve
                FeedbackCard(
                    title = "Areas to Improve",
                    items = listOf(
                        "Reduce filler words usage",
                        "Provide more specific examples",
                        "Structure answers using STAR method"
                    ),
                    iconColor = Color(0xFFFF9500),
                    iconRes = R.drawable.help
                )

                Spacer(Modifier.height(16.dp))

                // Speech Analysis (фиктивные данные)
                SpeechAnalysisCard(
                    paceLabel = "Optimal",
                    fillerWordsCount = 12
                )

                Spacer(Modifier.height(16.dp))

                // Scores Breakdown (фиктивные данные)
                ScoresCard(
                    communication = 92.0,
                    technical = 88.0,
                    confidence = 90.0
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    items: List<String>,
    iconColor: Color,
    iconRes: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.height(12.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(iconColor)
                            .align(Alignment.Top)
                            .offset(y = 8.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeechAnalysisCard(
    paceLabel: String,
    fillerWordsCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back), // сюда потом поставишь иконку чата/текста
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Speech Analysis",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            MetricRow(
                label = "Pace",
                value = paceLabel,
                isOptimal = paceLabel == "Optimal"
            )

            Spacer(Modifier.height(8.dp))

            MetricRow(
                label = "Filler Words",
                value = "$fillerWordsCount times",
                isOptimal = fillerWordsCount < 5
            )
        }
    }
}

@Composable
private fun ScoresCard(
    communication: Double,
    technical: Double,
    confidence: Double
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detailed Scores",
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(16.dp))

            ScoreBar("Communication", communication)
            Spacer(Modifier.height(12.dp))
            ScoreBar("Technical", technical)
            Spacer(Modifier.height(12.dp))
            ScoreBar("Confidence", confidence)
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isOptimal: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
        Text(
            text = value,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isOptimal) Color(0xFF34C759) else Color(0xFFFF9500)
        )
    }
}

@Composable
private fun ScoreBar(label: String, score: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = "${score.toInt()}%",
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE5E5EA))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (score / 100.0).toFloat())
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0A0932))
            )
        }
    }
}
