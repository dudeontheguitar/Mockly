package com.example.mocklyapp.presentation.interview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.domain.report.model.InterviewReport
import com.example.mocklyapp.presentation.theme.Poppins
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun InterviewResultsScreen(
    viewModel: InterviewResultsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPolling()
    }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            ResultsHeader(onBack = onBack)

            when {
                state.isLoading -> {
                    LoadingContent(
                        text = state.message ?: "Analyzing your interview..."
                    )
                }

                state.noReportReason != null -> {
                    NoReportContent(
                        reason = state.noReportReason ?: "No results available.",
                        onBack = onBack
                    )
                }

                state.error != null -> {
                    ErrorContent(
                        error = state.error ?: "Failed to load results.",
                        onRetry = { viewModel.retry() }
                    )
                }

                state.report != null -> {
                    ReportContent(
                        report = state.report!!,
                        message = state.message
                    )
                }

                state.isWaitingForReport -> {
                    WaitingReportContent(
                        message = state.message
                            ?: "Waiting for interview recording and report generation...",
                        onRetry = { viewModel.retry() }
                    )
                }

                else -> {
                    WaitingReportContent(
                        message = "Report is not ready yet.",
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsHeader(
    onBack: () -> Unit
) {
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
}

@Composable
private fun LoadingContent(
    text: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(16.dp))

            Text(
                text = text,
                fontFamily = Poppins,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun WaitingReportContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Report is not ready yet",
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text("Retry", fontFamily = Poppins)
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Failed to load results",
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = error,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = Color.Red
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text("Retry", fontFamily = Poppins)
            }
        }
    }
}

@Composable
private fun ReportContent(
    report: InterviewReport,
    message: String?
) {
    val metrics = report.metrics.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        StatusBadge(status = report.status)

        if (report.status == "PENDING" || report.status == "PROCESSING") {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = message ?: "Report is being generated, please wait...",
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (metrics.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            OverallCard(metrics = metrics)
        }

        if (!report.summary.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))

            ReportSection(title = "Summary") {
                Text(
                    text = report.summary,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    lineHeight = 22.sp
                )
            }
        }

        if (!report.recommendations.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))

            ReportSection(title = "Recommendations") {
                Text(
                    text = report.recommendations,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    lineHeight = 22.sp
                )
            }
        }

        if (metrics.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            ScoresCard(metrics = metrics)

            Spacer(Modifier.height(16.dp))
            StrengthsCard(metrics = metrics)

            Spacer(Modifier.height(16.dp))
            ImprovementsCard(metrics = metrics)

            Spacer(Modifier.height(16.dp))
            SpeechAnalysisCard(metrics = metrics)

            val extraMetrics = collectExtraMetrics(metrics)
            if (extraMetrics.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                ReportSection(title = "Other Metrics") {
                    extraMetrics.forEach { (key, value) ->
                        MetricRow(
                            key = prettifyKey(key),
                            value = formatMetricValue(value)
                        )
                    }
                }
            }
        }

        if (!report.errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))

            ReportSection(title = "Error Details") {
                Text(
                    text = report.errorMessage,
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = Color(0xFFFF3B30)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun OverallCard(
    metrics: Map<String, Any>
) {
    val overallScore = numberValue(metrics["overallScore"])
    val overallLabel = stringValue(metrics["overallLabel"])
    val overallMessage = stringValue(metrics["overallMessage"])

    ReportSection(title = "Overall Result") {
        if (overallScore != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatScore(overallScore),
                    fontFamily = Poppins,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = "/ 100",
                    fontFamily = Poppins,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = (overallScore / 100.0).toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }

        if (!overallLabel.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))

            Text(
                text = overallLabel,
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }

        if (!overallMessage.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = overallMessage,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun ScoresCard(
    metrics: Map<String, Any>
) {
    val scores = mapValue(metrics["scores"])

    if (scores.isNullOrEmpty()) return

    ReportSection(title = "Scores") {
        ScoreRow(
            title = "Technical",
            score = numberValue(scores["technical"])
        )

        ScoreRow(
            title = "Confidence",
            score = numberValue(scores["confidence"])
        )

        ScoreRow(
            title = "Communication",
            score = numberValue(scores["communication"])
        )
    }
}

@Composable
private fun ScoreRow(
    title: String,
    score: Double?
) {
    val safeScore = score ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Text(
                text = if (score == null) "-" else formatScore(safeScore),
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = (safeScore / 100.0).toFloat().coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun StrengthsCard(
    metrics: Map<String, Any>
) {
    val strengths = listValue(metrics["strengths"])
        .mapNotNull { stringValue(it) }
        .filter { it.isNotBlank() }

    if (strengths.isEmpty()) return

    ReportSection(title = "Strengths") {
        strengths.forEachIndexed { index, item ->
            BulletText(text = item)

            if (index != strengths.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ImprovementsCard(
    metrics: Map<String, Any>
) {
    val improvements = listValue(metrics["areasToImprove"])
        .mapNotNull { stringValue(it) }
        .filter { it.isNotBlank() }

    if (improvements.isEmpty()) return

    ReportSection(title = "Areas to Improve") {
        improvements.forEachIndexed { index, item ->
            BulletText(text = item)

            if (index != improvements.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SpeechAnalysisCard(
    metrics: Map<String, Any>
) {
    val speech = mapValue(metrics["speechAnalysis"])

    if (speech.isNullOrEmpty()) return

    ReportSection(title = "Speech Analysis") {
        MetricRow(
            key = "Pace",
            value = stringValue(speech["paceLabel"]) ?: "Unknown"
        )

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
        )

        MetricRow(
            key = "Pace Score",
            value = numberValue(speech["paceScore"])?.let { formatScore(it) } ?: "-"
        )

        MetricRow(
            key = "Filler Words",
            value = numberValue(speech["fillerWordsCount"])?.toInt()?.toString() ?: "0"
        )

        MetricRow(
            key = "Filler Word Rate",
            value = numberValue(speech["fillerWordRate"])?.let { "%.2f%%".format(it) } ?: "0%"
        )
    }
}

@Composable
private fun BulletText(
    text: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "•",
            fontFamily = Poppins,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = text,
            fontFamily = Poppins,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primaryContainer,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun StatusBadge(
    status: String
) {
    val statusColor = when (status.uppercase()) {
        "READY" -> Color(0xFF34C759)
        "FAILED" -> Color(0xFFFF3B30)
        "PROCESSING" -> Color(0xFF007AFF)
        "PENDING" -> Color(0xFFFF9500)
        else -> Color(0xFFFF9500)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status.uppercase(),
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = statusColor
            )
        }
    }
}

@Composable
private fun MetricRow(
    key: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            fontFamily = Poppins,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = value,
            fontFamily = Poppins,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReportSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

private fun collectExtraMetrics(metrics: Map<String, Any>): Map<String, Any> {
    val knownKeys = setOf(
        "scores",
        "strengths",
        "overallLabel",
        "overallScore",
        "areasToImprove",
        "overallMessage",
        "speechAnalysis"
    )

    return metrics.filterKeys { key -> key !in knownKeys }
}

private fun mapValue(value: Any?): Map<*, *>? {
    return value as? Map<*, *>
}

private fun listValue(value: Any?): List<*> {
    return value as? List<*> ?: emptyList<Any>()
}

private fun numberValue(value: Any?): Double? {
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

private fun stringValue(value: Any?): String? {
    return when (value) {
        null -> null
        is String -> value
        is Number -> formatMetricValue(value)
        else -> value.toString()
    }
}

private fun formatScore(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}

private fun formatMetricValue(value: Any?): String {
    return when (value) {
        null -> "-"
        is Double -> {
            if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                "%.2f".format(value)
            }
        }

        is Float -> {
            if (value % 1f == 0f) {
                value.toInt().toString()
            } else {
                "%.2f".format(value)
            }
        }

        is Number -> value.toString()

        is Map<*, *> -> {
            value.entries.joinToString(", ") { entry ->
                "${prettifyKey(entry.key.toString())}: ${formatMetricValue(entry.value)}"
            }
        }

        is List<*> -> {
            value.joinToString(", ") { item ->
                formatMetricValue(item)
            }
        }

        else -> value.toString()
    }
}

private fun prettifyKey(key: String): String {
    return key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.uppercase() }
}

@Composable
private fun NoReportContent(
    reason: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Results",
                    fontFamily = Poppins,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = reason,
                    fontFamily = Poppins,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Back",
                        fontFamily = Poppins,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}