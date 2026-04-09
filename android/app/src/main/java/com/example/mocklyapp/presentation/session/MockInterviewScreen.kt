package com.example.mocklyapp.presentation.session

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.theme.Poppins
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MockInterviewScreen(
    sessionId: String,
    onBack: () -> Unit,
    onEndInterview: (sessionId: String) -> Unit,
    artifactRepository: com.example.mocklyapp.domain.artifact.ArtifactRepository
)
{
    val scope = rememberCoroutineScope()

    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // наш рекордер
    val recorder = remember { InterviewAudioRecorder(context) }

    var hasMicPermission by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableStateOf(0) }
    var lastFile by remember { mutableStateOf<File?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // permission launcher
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasMicPermission = granted
            if (granted) {
                // как только дали доступ — стартуем запись
                try {
                    lastFile = recorder.start(sessionId)
                    isRecording = true
                    elapsedSec = 0
                } catch (e: Exception) {
                    errorText = e.message ?: "Failed to start recording"
                    isRecording = false
                }
            } else {
                errorText = "Microphone permission is required for mock interview."
            }
        }

    // запрашиваем доступ к микрофону при первом входе
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // таймер — пока идёт запись
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        while (isRecording) {
            delay(1000)
            elapsedSec += 1
        }
    }

    // освобождаем ресурсы при выходе с экрана
    DisposableEffect(Unit) {
        onDispose {
            recorder.stop()
            recorder.release()
        }
    }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(16.dp)
        ) {

            // Основное "видео" кандидата (заглушка)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) "Recording..." else "Waiting...",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )

                // маленькое окошко интервьюера
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(110.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Interviewer",
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                }
            }

            // нижняя панель с кнопками
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                // время записи
                Text(
                    text = formatTime(elapsedSec),
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // End call (красная)
                    // End call (красная)
                    CircleIconButton(
                        background = Color(0xFFE54B4B),
                        iconRes = R.drawable.call_end
                    ) {
                        // стоп записи
                        isRecording = false
                        val file = recorder.stop()
                        lastFile = file

                        if (file == null) {
                            // ничего не записали — просто выходим
                            onEndInterview(sessionId)
                        } else {
                            isUploading = true
                            scope.launch {
                                try {
                                    artifactRepository.uploadSessionAudio(
                                        sessionId = sessionId,
                                        file = file,
                                        durationSec = elapsedSec
                                    )
                                    onEndInterview(sessionId)
                                } catch (e: Exception) {
                                    errorText = e.message ?: "Failed to upload interview"
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    }


                    Spacer(Modifier.width(18.dp))

                    // Camera toggle (пока просто UI)
                    CircleIconButton(
                        background = Color(0xFF0A0932),
                        iconRes = R.drawable.videocam
                    ) {
                        // TODO: камера позже, сейчас только дизайн
                    }

                    Spacer(Modifier.width(18.dp))

                    // Mic toggle (останавливает/возобновляет запись)
                    CircleIconButton(
                        background = Color(0xFF0A0932),
                        iconRes = if (isRecording) R.drawable.mic_off else R.drawable.mic
                    ) {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isRecording) {
                                isRecording = false
                                recorder.stop()
                            } else {
                                try {
                                    lastFile = recorder.start(sessionId)
                                    elapsedSec = 0
                                    isRecording = true
                                } catch (e: Exception) {
                                    errorText = "${e::class.simpleName}: ${e.message ?: "Failed to upload interview"}"
                                }

                            }
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    // More button (ничего не делает, только UI)
                    CircleIconButton(
                        background = Color(0xFF0A0932),
                        iconRes = R.drawable.more_horiz
                    ) {
                        // TODO: опции позже
                    }
                }

                Spacer(Modifier.height(18.dp))

                // снизу "плашка" с текстом интервьюера
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Hello, let's start the mock interview...",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Interviewer",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Uploading interview...",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // ошибка (если что-то пошло не так)
            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }

        }
    }
}

@Composable
private fun CircleIconButton(
    background: Color,
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(background)
            .padding(14.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
