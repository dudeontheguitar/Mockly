package com.example.mocklyapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    onChangePasswordClick: () -> Unit = {},
    onHelpCenterClick: () -> Unit = {},
    onContactSupportClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLogoutClick()
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

            item {
                val fullName = "${state.name} ${state.surname}".trim()
                UserProfileCard(
                    fullName = fullName.ifBlank { "Your Name" },
                    email = state.email.ifBlank { "example@gmail.com" },
                )
            }

            if (state.error != null) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Text(
                        text = state.error ?: "",
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item { SectionTitle("Account") }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                AccountSection(
                    onEditProfileClick = onEditProfileClick,
                    onChangePasswordClick = onChangePasswordClick
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }


            item { SectionTitle("App Settings") }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                AppSettingsSection(
                    notificationsEnabled = state.notificationsEnabled,
                    darkModeEnabled = state.darkModeEnabled,
                    onNotificationsChange = { viewModel.onNotificationsChange(it) },
                    onDarkModeChange = { viewModel.onDarkModeChange(it) }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }


            item { SectionTitle("Support") }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                SupportSection(
                    onHelpCenterClick = onHelpCenterClick,
                    onContactSupportClick = onContactSupportClick,
                    onTermsClick = onTermsClick
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                LogoutButton(
                    onClick = { viewModel.logout() }
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Settings",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            style = TextStyle(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            )
        )
    }
}

@Composable
private fun UserProfileCard(
    fullName: String,
    email: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                    Text(
                        text = email,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSection(
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            SettingsItemInCard(
                title = "Edit Profile",
                iconRes = R.drawable.profile,
                hasArrow = true,
                onClick = onEditProfileClick
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFFE5E5E5)
            )
            SettingsItemInCard(
                title = "Change Password",
                iconRes = R.drawable.password,
                hasArrow = true,
                onClick = onChangePasswordClick
            )
        }
    }
}

@Composable
private fun AppSettingsSection(
    notificationsEnabled: Boolean,
    darkModeEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            SettingsItemWithSwitchInCard(
                title = "Notifications",
                iconRes = R.drawable.notification,
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFFE5E5E5)
            )
            SettingsItemWithSwitchInCard(
                title = "Dark Mode",
                iconRes = R.drawable.dark_mode,
                checked = darkModeEnabled,
                onCheckedChange = onDarkModeChange
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFFE5E5E5)
            )
            SettingsItemInCard(
                title = "Language",
                iconRes = R.drawable.language,
                hasArrow = true,
                onClick = { /* TODO: open language screen */ }
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFFE5E5E5)
            )
            SettingsItemInCard(
                title = "Privacy & Security",
                iconRes = R.drawable.privacy,
                hasArrow = true,
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun SupportSection(
    onHelpCenterClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            SettingsItemInCard(
                title = "Help Center",
                iconRes = R.drawable.help,
                hasArrow = true,
                onClick = onHelpCenterClick
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFFE5E5E5)
            )
            SettingsItemInCard(
                title = "Contact Support",
                iconRes = R.drawable.support,
                hasArrow = true,
                onClick = onContactSupportClick
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFFE5E5E5)
            )
            SettingsItemInCard(
                title = "Terms & Policies",
                iconRes = R.drawable.term,
                hasArrow = true,
                onClick = onTermsClick
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
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
private fun SettingsItemWithSwitchInCard(
    title: String,
    iconRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1E1E2D),
                uncheckedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedTrackColor = Color.White,
                uncheckedBorderColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
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
