package com.example.mocklyapp.presentation.session

import android.Manifest
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
import com.example.mocklyapp.domain.session.SessionRepository
import com.example.mocklyapp.presentation.theme.Poppins
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MockInterviewScreen(
    sessionId: String,
    onBack: () -> Unit,
    onEndInterview: (sessionId: String) -> Unit,
    sessionRepository: SessionRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissionsGranted by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var isEnding by remember { mutableStateOf(false) }

    var elapsedSec by remember { mutableStateOf(0) }

    var isMicEnabled by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(true) }

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

        try {
            sessionRepository.joinSession(sessionId)

            val liveKitToken = sessionRepository.getLiveKitToken(sessionId)
            val liveKitUrl = fixLocalhostForEmulator(liveKitToken.url)

            val liveKitRoom = LiveKit.create(appContext = context)

            liveKitRoom.connect(
                url = liveKitUrl,
                token = liveKitToken.token
            )

            liveKitRoom.localParticipant.setMicrophoneEnabled(true)
            liveKitRoom.localParticipant.setCameraEnabled(true)

            room = liveKitRoom
            isConnected = true
            isMicEnabled = true
            isCameraEnabled = true
            elapsedSec = 0
        } catch (e: Exception) {
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

    DisposableEffect(Unit) {
        onDispose {
            CoroutineScope(Dispatchers.Main).launch {
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
                                text = "LiveKit room connected",
                                style = TextStyle(
                                    fontFamily = Poppins,
                                    fontSize = 14.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f)
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
                                onEndInterview(sessionId)
                            } catch (e: Exception) {
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
                        // TODO: options
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
                            text = "LiveKit session. Recording is handled by backend.",
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
                            text = "Ending interview...",
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

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
