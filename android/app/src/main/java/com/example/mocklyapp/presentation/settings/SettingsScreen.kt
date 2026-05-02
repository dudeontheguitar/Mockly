package com.example.mocklyapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.theme.Poppins

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEditProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLogoutClick()
        }
    }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Header() }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                state.error != null -> {
                    item {
                        ErrorCard(
                            error = state.error ?: "Failed to load settings.",
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                else -> {
                    item {
                        UserProfileCard(
                            fullName = "${state.name} ${state.surname}".trim().ifBlank { "User" },
                            email = state.email,
                            role = state.role,
                            level = state.level,
                            location = state.location
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item { SectionTitle("Profile") }

                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    item {
                        ProfileDetailsCard(
                            bio = state.bio,
                            skills = state.skills
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item { SectionTitle("Account") }

                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    item {
                        AccountSection(
                            onEditProfileClick = onEditProfileClick
                        )
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }

                    item {
                        LogoutButton(onClick = { viewModel.logout() })
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = "Settings",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        style = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp
        )
    )
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Failed to load profile",
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

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onRetry) {
                Text("Retry", fontFamily = Poppins)
            }
        }
    }
}

@Composable
private fun UserProfileCard(
    fullName: String,
    email: String,
    role: String,
    level: String?,
    location: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8E8E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = fullName,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = email,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )

                Spacer(Modifier.height(6.dp))

                val subtitle = buildString {
                    append(role.ifBlank { "USER" })
                    if (!level.isNullOrBlank()) append(" • $level")
                    if (!location.isNullOrBlank()) append(" • $location")
                }

                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileDetailsCard(
    bio: String?,
    skills: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bio",
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = bio?.takeIf { it.isNotBlank() } ?: "No bio added yet.",
                fontFamily = Poppins,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(14.dp))

            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5E5))

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Skills",
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (skills.isEmpty()) {
                    "No skills added yet."
                } else {
                    skills.joinToString(", ")
                },
                fontFamily = Poppins,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun AccountSection(
    onEditProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            SettingsItemInCard(
                title = "Edit Profile",
                iconRes = R.drawable.profile,
                hasArrow = true,
                onClick = onEditProfileClick
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        style = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun SettingsItemInCard(
    title: String,
    iconRes: Int,
    hasArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        if (hasArrow) {
            Icon(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = "Arrow",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logout),
                contentDescription = "Logout",
                modifier = Modifier.size(20.dp),
                tint = Color.Red
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Logout",
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.Red
                )
            )
        }
    }
}