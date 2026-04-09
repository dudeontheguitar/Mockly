package com.example.mocklyapp.presentation.auth.register

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

data class RegisterUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "candidate",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class RegisterViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, error = null)
    }

    fun onSurnameChange(value: String) {
        _state.value = _state.value.copy(surname = value, error = null)
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun onRoleChange(value: String) {
        _state.value = _state.value.copy(role = value, error = null)
    }

    fun onRegisterClick() {
        val s = _state.value

        if (s.name.isBlank() ||
            s.surname.isBlank() ||
            s.email.isBlank() ||
            s.password.isBlank()
        ) {
            setError("Please fill in all fields.")
            return
        }
        if (!s.name.matches(Regex("^[A-Za-zА-Яа-яЁё]+$"))) {
            setError("Name must contain only letters.")
            return
        }

        if (!s.surname.matches(Regex("^[A-Za-zА-Яа-яЁё]+$"))) {
            setError("Name must contain only letters.")
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

        if (s.role.isBlank()) {
            setError("Please select your role.")
            return
        }

        val backendRole = when (s.role.lowercase()) {
            "candidate" -> "CANDIDATE"
            "interviewer" -> "INTERVIEWER"
            else -> {
                setError("Invalid role selected.")
                return
            }
        }

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)

            try {

                repo.register(
                    email = s.email,
                    password = s.password,
                    name = s.name,
                    surname = s.surname,
                    role = backendRole
                )

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
        _state.value = _state.value.copy(
            isLoading = false,
            error = msg,
            isSuccess = false
        )
    }

    private fun mapError(e: Throwable): String {
        return when (e) {
            is HttpException -> when (e.code()) {
                400 -> "Invalid request. Please check your input."
                401 -> "You are not authorized to perform this action."
                403 -> "You do not have permission to register with this role."
                404 -> "Service not found."
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
