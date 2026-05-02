package com.example.mocklyapp.presentation.settings.edit_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.theme.Poppins

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var name by remember(state.name) { mutableStateOf(state.name) }
    var surname by remember(state.surname) { mutableStateOf(state.surname) }
    var level by remember(state.level) { mutableStateOf(state.level) }
    var skillsText by remember(state.skillsText) { mutableStateOf(state.skillsText) }
    var bio by remember(state.bio) { mutableStateOf(state.bio) }
    var location by remember(state.location) { mutableStateOf(state.location) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onBack()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TopBar(onBack = onBack)

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8E8E8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.profile),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(50.dp),
                                    tint = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            ProfileTextField(
                                label = "Name",
                                value = name,
                                onValueChange = { name = it }
                            )

                            ProfileTextField(
                                label = "Surname",
                                value = surname,
                                onValueChange = { surname = it }
                            )

                            ProfileTextField(
                                label = "Email",
                                value = state.email,
                                onValueChange = {},
                                enabled = false
                            )

                            ProfileTextField(
                                label = "Level",
                                value = level,
                                onValueChange = { level = it },
                                placeholder = "Junior, Middle, Senior..."
                            )

                            ProfileTextField(
                                label = "Skills",
                                value = skillsText,
                                onValueChange = { skillsText = it },
                                placeholder = "Kotlin, Java, Spring"
                            )

                            ProfileTextField(
                                label = "Location",
                                value = location,
                                onValueChange = { location = it },
                                placeholder = "Almaty, Kazakhstan"
                            )

                            ProfileTextField(
                                label = "Bio",
                                value = bio,
                                onValueChange = { bio = it },
                                placeholder = "Tell something about yourself",
                                minLines = 3
                            )

                            state.error?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = error,
                                    color = Color.Red,
                                    fontFamily = Poppins,
                                    fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    viewModel.saveProfile(
                                        name = name,
                                        surname = surname,
                                        level = level,
                                        skillsText = skillsText,
                                        bio = bio,
                                        location = location
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !state.isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "Save",
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit
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
            text = "Edit Profile",
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    placeholder: String = "",
    minLines: Int = 1
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = minLines == 1,
            minLines = minLines,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = Poppins,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedContainerColor = Color(0xFFF5F5F5),
                disabledContainerColor = Color(0xFFF5F5F5),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text
            )
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}