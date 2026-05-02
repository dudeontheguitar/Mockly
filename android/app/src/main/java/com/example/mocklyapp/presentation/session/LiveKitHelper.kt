package com.example.mocklyapp.presentation.session

fun fixLocalhostForEmulator(url: String): String {
    return url
        .replace("ws://localhost", "ws://10.0.2.2")
        .replace("wss://localhost", "wss://10.0.2.2")
        .replace("http://localhost", "http://10.0.2.2")
        .replace("https://localhost", "https://10.0.2.2")
        .replace("ws://127.0.0.1", "ws://10.0.2.2")
        .replace("wss://127.0.0.1", "wss://10.0.2.2")
        .replace("http://127.0.0.1", "http://10.0.2.2")
        .replace("https://127.0.0.1", "https://10.0.2.2")
}