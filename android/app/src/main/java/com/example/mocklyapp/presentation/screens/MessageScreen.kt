package com.example.mocklyapp.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.mocklyapp.models.MessageItem
import com.example.mocklyapp.presentation.theme.Poppins
import kotlinx.coroutines.launch

@Composable
fun MessageScreen() {

    val messages = listOf(
        MessageItem("Batyrkhan Malikov", "Excellent. See you tomorrow...", "12:56", 2),
        MessageItem("Azamat Ashmakyn", "Thank you!", "12:34", 0),
        MessageItem("Daniyar Kulumshin", "Ok", "11:43", 1),
        MessageItem("Yerbolat Bedelkhan", "Product management is for...", "11:43", 0),
        MessageItem("Baglan Nurkassym", "Minecraft is good game", "11:21", 3),
        MessageItem("Aibar Barlykov", "I also play minecraft everyday", "11:10", 0),
        MessageItem("Abdulgazimov Nurbek", "Aktobe sila", "10:53", 1),
        MessageItem("Utessinov Arkat", "Thank you!", "10:48", 0),
        MessageItem("Aliev Yerassyl", "AAAAA", "10:21", 8),
        MessageItem("Atullaev Gaziz", "I am middle backend dev...", "9:43", 0),
    )

    Surface (color = MaterialTheme.colorScheme.onBackground){
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item { Header() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                MessageCard(messages = messages)
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
            text = "Chats",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            style = TextStyle(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            ))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSearchBar(
    modifier: Modifier = Modifier
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = remember { TextFieldState() }
    val scope = rememberCoroutineScope()

    val inputColors = SearchBarDefaults.inputFieldColors(
        focusedTextColor = MaterialTheme.colorScheme.primaryContainer,
        unfocusedTextColor = MaterialTheme.colorScheme.primaryContainer,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primaryContainer,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.primaryContainer,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
    )

    val barColors = SearchBarDefaults.colors(
        containerColor = Color.White,
        dividerColor = Color.Transparent,
        inputFieldColors = inputColors
    )

    Box(modifier = modifier) {

        SearchBar(
            state = searchBarState,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = barColors,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = { query ->
                        // TODO: фильтровать список по query
                        scope.launch { searchBarState.animateToCollapsed() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search conversations",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp
                            )) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = inputColors
                )
            }
        )

        ExpandedFullScreenSearchBar(
            state = searchBarState,
            modifier = Modifier.fillMaxSize(),
            colors = barColors,
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = { query ->
                        scope.launch { searchBarState.animateToCollapsed() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search conversations",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            style = TextStyle(
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp
                            )) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = inputColors
                )
            },
            content = {

            }
        )
    }
}

@Composable
fun MessageCard(messages: List<MessageItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        MessageSearchBar()

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFFE5E5E5)
        )
        Spacer(Modifier.height(8.dp))

        messages.forEachIndexed { index, item ->
            ChatItem(item)

            if (index != messages.lastIndex) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color(0xFFE5E5E5),
                    modifier = Modifier.padding(start = 60.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ChatItem(item: MessageItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар
        Box(
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.alem),
                contentDescription = "",
                modifier = Modifier.size(65.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Имя + последнее сообщение
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.author,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            )
            Text(
                text = item.lastMessage,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            )
        }

        // Время + кружок непрочитанных
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = item.time,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            if (item.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.unreadCount.toString(),
                        color = Color.White,
                        style = TextStyle(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

