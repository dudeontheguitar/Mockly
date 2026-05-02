package com.example.mocklyapp.presentation.session

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.mocklyapp.domain.artifact.ArtifactRepository
import com.example.mocklyapp.domain.report.ReportRepository
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.presentation.theme.Poppins
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MockInterviewScreen(
    sessionId: String,
    onBack: () -> Unit,
    onEndInterview: (sessionId: String, noReportReason: String?) -> Unit,
    sessionRepository: SessionRepository,
    artifactRepository: ArtifactRepository,
    reportRepository: ReportRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val audioRecorder = remember {
        LocalInterviewAudioRecorder(context.applicationContext)
    }

    var permissionsGranted by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var isEnding by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    var elapsedSec by remember { mutableIntStateOf(0) }

    var isMicEnabled by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(true) }
    var hasRemoteParticipantJoined by remember { mutableStateOf(false) }

    var room by remember { mutableStateOf<Room?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val cameraOk = perms[Manifest.permission.CAMERA] == true
        val audioOk = perms[Manifest.permission.RECORD_AUDIO] == true

        permissionsGranted = cameraOk && audioOk

        if (!permissionsGranted) {
            errorText = "Camera and microphone permissions are required for the interview."
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    LaunchedEffect(permissionsGranted) {
        if (!permissionsGranted) return@LaunchedEffect
        if (room != null) return@LaunchedEffect

        isConnecting = true
        errorText = null
        hasRemoteParticipantJoined = false

        try {
            sessionRepository.joinSession(sessionId)

            val liveKitToken = sessionRepository.getLiveKitToken(sessionId)
            val liveKitUrl = "wss://rtc.iness.app"

            val liveKitRoom = LiveKit.create(appContext = context)

            liveKitRoom.connect(
                url = liveKitUrl,
                token = liveKitToken.token
            )

            liveKitRoom.localParticipant.setMicrophoneEnabled(true)
            liveKitRoom.localParticipant.setCameraEnabled(true)

            try {
                audioRecorder.start()
                isRecording = true
                Log.d("MockInterviewScreen", "Local audio recording started")
            } catch (e: Exception) {
                Log.e("MockInterviewScreen", "Failed to start local audio recording", e)
                errorText = "Interview connected, but audio recording failed: ${e.message}"
            }

            room = liveKitRoom
            isConnected = true
            isMicEnabled = true
            isCameraEnabled = true
            elapsedSec = 0
        } catch (e: Exception) {
            Log.e("MockInterviewScreen", "Failed to join interview", e)
            errorText = "Failed to join interview: ${e.message}"
            isConnected = false
        } finally {
            isConnecting = false
        }
    }

    LaunchedEffect(isConnected) {
        if (!isConnected) return@LaunchedEffect

        while (isConnected) {
            delay(1000)
            elapsedSec++
        }
    }

    LaunchedEffect(isConnected, room) {
        if (!isConnected) return@LaunchedEffect

        while (isConnected) {
            val currentRoom = room

            if (currentRoom != null && currentRoom.remoteParticipants.isNotEmpty()) {
                hasRemoteParticipantJoined = true
                Log.d("MockInterviewScreen", "Remote participant detected")
            }

            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    if (isRecording) {
                        audioRecorder.stop()
                    }
                } catch (_: Exception) {
                }

                try {
                    room?.localParticipant?.setMicrophoneEnabled(false)
                } catch (_: Exception) {
                }

                try {
                    room?.localParticipant?.setCameraEnabled(false)
                } catch (_: Exception) {
                }

                try {
                    room?.disconnect()
                } catch (_: Exception) {
                }

                audioRecorder.release()
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isConnecting -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Connecting to interview...",
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    isConnected -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Interview in progress",
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (isRecording) {
                                    "LiveKit connected. Audio recording active."
                                } else {
                                    "LiveKit connected. Audio recording inactive."
                                },
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 14.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = if (hasRemoteParticipantJoined) {
                                    "Second participant connected."
                                } else {
                                    "Waiting for second participant."
                                },
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 13.sp
                                ),
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = "Waiting for permissions...",
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }
                }

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
                        text = "You",
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
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
                    CircleIconButton(
                        background = Color(0xFFE54B4B),
                        iconRes = R.drawable.call_end
                    ) {
                        if (isEnding) return@CircleIconButton

                        isEnding = true
                        errorText = null

                        scope.launch {
                            try {
                                val durationSec = elapsedSec.coerceAtLeast(1)

                                val audioFile = try {
                                    if (isRecording) {
                                        isRecording = false
                                        audioRecorder.stop()
                                    } else {
                                        audioRecorder.currentFile()
                                    }
                                } catch (e: Exception) {
                                    Log.e("MockInterviewScreen", "Failed to stop audio recording", e)
                                    throw IllegalStateException(
                                        "Failed to stop audio recording: ${e.message}",
                                        e
                                    )
                                }

                                try {
                                    room?.localParticipant?.setMicrophoneEnabled(false)
                                } catch (_: Exception) {
                                }

                                try {
                                    room?.localParticipant?.setCameraEnabled(false)
                                } catch (_: Exception) {
                                }

                                sessionRepository.endSession(sessionId)

                                try {
                                    room?.disconnect()
                                } catch (_: Exception) {
                                }

                                isConnected = false

                                if (!hasRemoteParticipantJoined) {
                                    audioFile?.delete()

                                    Log.d(
                                        "MockInterviewScreen",
                                        "Report skipped: no remote participant joined"
                                    )

                                    onEndInterview(
                                        sessionId,
                                        "No results available because the second participant did not join the session."
                                    )

                                    return@launch
                                }

                                if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0L) {
                                    throw IllegalStateException("Audio file is empty. Report cannot be generated.")
                                }

                                artifactRepository.uploadSessionAudio(
                                    sessionId = sessionId,
                                    file = audioFile,
                                    durationSec = durationSec
                                )

                                runCatching {
                                    reportRepository.triggerSessionReport(sessionId)
                                }.onFailure { e ->
                                    Log.w(
                                        "MockInterviewScreen",
                                        "triggerSessionReport failed, result screen will continue polling",
                                        e
                                    )
                                }

                                onEndInterview(sessionId, null)
                            } catch (e: Exception) {
                                Log.e("MockInterviewScreen", "Failed to finish interview", e)
                                errorText = e.message ?: "Failed to end interview"
                                isEnding = false
                            }
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    CircleIconButton(
                        background = Color(0xFF0A0932),
                        iconRes = R.drawable.videocam
                    ) {
                        val currentRoom = room ?: return@CircleIconButton

                        scope.launch {
                            try {
                                val newValue = !isCameraEnabled
                                currentRoom.localParticipant.setCameraEnabled(newValue)
                                isCameraEnabled = newValue
                            } catch (e: Exception) {
                                errorText = e.message ?: "Failed to toggle camera"
                            }
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    CircleIconButton(
                        background = Color(0xFF0A0932),
                        iconRes = if (isMicEnabled) R.drawable.mic else R.drawable.mic_off
                    ) {
                        val currentRoom = room ?: return@CircleIconButton

                        scope.launch {
                            try {
                                val newValue = !isMicEnabled
                                currentRoom.localParticipant.setMicrophoneEnabled(newValue)
                                isMicEnabled = newValue
                            } catch (e: Exception) {
                                errorText = e.message ?: "Failed to toggle microphone"
                            }
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    CircleIconButton(
                        background = Color(0xFF0A0932),
                        iconRes = R.drawable.more_horiz
                    ) {
                    }
                }

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = when {
                                isEnding -> "Ending interview..."
                                isConnected -> "Interview in progress"
                                else -> "Connecting..."
                            },
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = when {
                                isEnding -> {
                                    if (hasRemoteParticipantJoined) {
                                        "Uploading audio and preparing report..."
                                    } else {
                                        "Closing session without report."
                                    }
                                }

                                isRecording -> "Audio is being recorded locally for report generation."
                                else -> "Audio recording is not active."
                            },
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

            if (isEnding) {
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
                            text = if (hasRemoteParticipantJoined) {
                                "Uploading audio..."
                            } else {
                                "Closing session..."
                            },
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            errorText?.let { err ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = err,
                        color = Color.Red,
                        fontFamily = Poppins,
                        modifier = Modifier.padding(12.dp)
                    )
                }
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

private class LocalInterviewAudioRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        release()

        val file = File(
            context.cacheDir,
            "interview_audio_${System.currentTimeMillis()}.m4a"
        )

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioEncodingBitRate(128_000)
        mediaRecorder.setAudioSamplingRate(44_100)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()

        recorder = mediaRecorder
        outputFile = file

        return file
    }

    fun stop(): File? {
        val mediaRecorder = recorder ?: return outputFile
        val file = outputFile

        try {
            mediaRecorder.stop()
        } catch (e: RuntimeException) {
            file?.delete()
            throw e
        } finally {
            runCatching {
                mediaRecorder.reset()
            }

            runCatching {
                mediaRecorder.release()
            }

            recorder = null
        }

        return file
    }

    fun currentFile(): File? {
        return outputFile
    }

    fun release() {
        val mediaRecorder = recorder

        if (mediaRecorder != null) {
            runCatching {
                mediaRecorder.reset()
            }

            runCatching {
                mediaRecorder.release()
            }
        }

        recorder = null
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}