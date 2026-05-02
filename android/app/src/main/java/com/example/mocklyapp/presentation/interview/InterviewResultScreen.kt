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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

        if (!report.summary.isNullOrBlank()) {
            Spacer(Modifier.height(20.dp))

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

        if (!report.metrics.isNullOrEmpty()) {
            Spacer(Modifier.height(16.dp))

            ReportSection(title = "Metrics") {
                report.metrics.forEach { (key, value) ->
                    MetricRow(
                        key = key,
                        value = formatMetricValue(value)
                    )
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
        is Map<*, *> -> {
            value.entries.joinToString(", ") { entry ->
                "${entry.key}: ${formatMetricValue(entry.value)}"
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