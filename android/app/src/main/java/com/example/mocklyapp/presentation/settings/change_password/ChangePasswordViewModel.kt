package com.example.mocklyapp.presentation.settings.change_password

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

data class ChangePasswordUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class ChangePasswordViewModel(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        when {
            currentPassword.isBlank() ||
                    newPassword.isBlank() ||
                    confirmPassword.isBlank() -> {
                _state.value = _state.value.copy(
                    error = "All fields are required."
                )
                return
            }

            newPassword.length < 6 -> {
                _state.value = _state.value.copy(
                    error = "New password is too short."
                )
                return
            }

            newPassword != confirmPassword -> {
                _state.value = _state.value.copy(
                    error = "Passwords do not match."
                )
                return
            }
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            try {
                authRepo.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
                _state.value = _state.value.copy(
                    isSaving = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = mapError(e)
                )
            }
        }
    }

    private fun mapError(e: Throwable): String {
        return when (e) {
            is HttpException -> when (e.code()) {
                400 -> "Current password is incorrect."
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
