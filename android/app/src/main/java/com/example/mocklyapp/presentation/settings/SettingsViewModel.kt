package com.example.mocklyapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.auth.AuthRepository
import com.example.mocklyapp.domain.user.UserRepository
import com.example.mocklyapp.domain.user.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class SettingsUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false
)

class SettingsViewModel(
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(isLoading = true))
    val state: StateFlow<SettingsUiState> = _state

    init {
        loadUser()
    }

    fun applyUser(user: User) {
        _state.value = _state.value.copy(
            name = user.name,
            surname = user.surname ?: "",
            email = user.email
        )
    }

    private fun loadUser() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val user = userRepo.getCurrentUser()
                _state.value = _state.value.copy(
                    isLoading = false,
                    name = user.name,
                    surname = user.surname ?: "",
                    email = user.email,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = mapError(e)
                )
            }
        }
    }

    fun onNotificationsChange(value: Boolean) {
        _state.value = _state.value.copy(notificationsEnabled = value)
    }

    fun onDarkModeChange(value: Boolean) {
        _state.value = _state.value.copy(darkModeEnabled = value)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepo.logout()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(isLoggedOut = true)
        }
    }

    private fun mapError(e: Throwable): String {
        return when (e) {
            is HttpException -> when (e.code()) {
                401 -> "Session expired. Please log in again."
                in 500..599 -> "Server error. Please try again later."
                else -> "Server error (HTTP ${e.code()})."
            }

            is UnknownHostException ->
                "Cannot reach the server. Check your internet connection."

            is SocketTimeoutException ->
                "The server took too long to respond. Try again."

            is IOException ->
                "Network error. Please check your internet connection."

            else ->
                "Unexpected error: ${e.message ?: "Something went wrong."}"
        }
    }
}
