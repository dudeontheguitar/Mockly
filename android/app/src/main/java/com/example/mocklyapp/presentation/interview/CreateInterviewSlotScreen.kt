package com.example.mocklyapp.presentation.interview

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.mocklyapp.presentation.theme.Poppins

@Composable
fun CreateInterviewSlotScreen(
    viewModel: CreateInterviewSlotViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onCreated()
        }
    }

    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Slot information",
                            fontFamily = Poppins,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(Modifier.height(16.dp))

                        AppTextField(
                            label = "Title",
                            value = state.title,
                            onValueChange = viewModel::setTitle
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = "Company",
                            value = state.company,
                            onValueChange = viewModel::setCompany
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = "Location",
                            value = state.location,
                            onValueChange = viewModel::setLocation
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = "Description",
                            value = state.description,
                            onValueChange = viewModel::setDescription,
                            minLines = 3
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Schedule",
                            fontFamily = Poppins,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(Modifier.height(16.dp))

                        AppTextField(
                            label = "Date yyyy-MM-dd",
                            value = state.date,
                            onValueChange = viewModel::setDate
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = "Time HH:mm",
                            value = state.time,
                            onValueChange = viewModel::setTime
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = "Duration minutes",
                            value = state.durationMinutes,
                            onValueChange = viewModel::setDurationMinutes
                        )
                    }
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = error,
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.createSlot() },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Create Slot",
                            fontFamily = Poppins,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
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
            text = "Create Interview Slot",
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontFamily = Poppins
            )
        },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontFamily = Poppins,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.primaryContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFFE0E0F0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}