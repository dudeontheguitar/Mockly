package com.example.mocklyapp.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun onLoginClick() {
        val s = _state.value

        if (s.email.isBlank() || s.password.isBlank()) {
            setError("Please fill in all fields.")
            return
        }

        val allowedDomains = listOf("@gmail.com", "@mail.ru", "@sdu.edu.kz")

        if (!allowedDomains.any { s.email.endsWith(it) }) {
            setError("Please enter a valid email.")
            return
        }


        if (s.password.length < 8) {
            setError("Password must be at least 8 characters long.")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)

            try {
                repo.login(s.email, s.password)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                val msg = mapError(e)
                setError(msg)
            }
        }
    }

    private fun setError(msg: String) {
        _state.value = _state.value.copy(isLoading = false, error = msg)
    }

    private fun mapError(e: Throwable): String {
        return when (e) {

            is HttpException -> when (e.code()) {
                400 -> "Invalid request. Please check your email and password."
                401 -> "Incorrect email or password."
                403 -> "You do not have permission to perform this action."
                404 -> "User not found."
                409 -> "This email is already registered."
                422 -> "Input validation failed."
                429 -> "Too many requests. Please try again later."
                in 500..599 -> "Server error. Please try again later."
                else -> "Server error (HTTP ${e.code()})."
            }

            is UnknownHostException ->
                "Cannot reach the server. Please check your internet connection."

            is SocketTimeoutException ->
                "The server took too long to respond. Try again."

            is IOException ->
                "Network error. Please check your internet connection."

            else ->
                "Unexpected error: ${e.message ?: "Something went wrong."}"
        }
    }
}
