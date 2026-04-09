package com.example.mocklyapp.presentation.auth.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mocklyapp.R
import com.example.mocklyapp.presentation.theme.Poppins

@Composable
fun OnboardingScreen1(
    onSkipClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding1),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .width(304.dp)
                    .height(290.dp)
            )

            Spacer(Modifier.height(50.dp))

            Text(
                text = "Welcome to Mockly",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                )
            )

            Spacer(Modifier.height(30.dp))


            Text(
                text = "Practice real interviews in a safe\nand friendly space\nImprove your confidence and\ncommunication skills",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {onSkipClick()},
                ){
                    Text(text = "skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }


                Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                }

                TextButton(
                    onClick = {onNextClick()},
                ){
                    Text(text = "next",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen2(
    onSkipClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding2),
                contentDescription = null,
                modifier = Modifier
                    .width(304.dp)
                    .height(290.dp)
            )

            Spacer(Modifier.height(50.dp))

            Text(
                text = "Practice with Real\nPeople",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                )
            )

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Connect with candidates and\ninterviewers from anywhere\nGet real experience before your\nnext job interview",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onSkipClick()}) {
                    Text(
                        text = "skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                }

                TextButton(onClick = {onNextClick()}) {
                    Text(
                        text = "next",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
@Composable
fun OnboardingScreen3(
    onSkipClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.onBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding3),
                contentDescription = null,
                modifier = Modifier
                    .width(304.dp)
                    .height(290.dp)
            )

            Spacer(Modifier.height(50.dp))

            Text(
                text = "Get AI Feedback\nInstantly",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                )
            )

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Receive automatic reports\nabout your speech:\npace, pauses, fillers, and\nimprovement tips",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {onSkipClick()}) {
                    Text(
                        text = "skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }

                TextButton(onClick = {onNextClick()}) {
                    Text(
                        text = "next",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}