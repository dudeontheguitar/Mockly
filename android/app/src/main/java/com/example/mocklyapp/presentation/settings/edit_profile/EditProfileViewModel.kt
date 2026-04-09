package com.example.mocklyapp.presentation.settings.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mocklyapp.domain.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class EditProfileUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class EditProfileViewModel(
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState(isLoading = true))
    val state: StateFlow<EditProfileUiState> = _state

    init {
        loadUser()
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
                    avatarUrl = user.avatarUrl,
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

    fun saveProfile(
        name: String,
        surname: String
    ) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(
                error = "Name cannot be empty."
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val updated = userRepo.updateCurrentUser(
                    name = name,
                    surname = surname,
                    avatarUrl = _state.value.avatarUrl, // пока просто прокидываем текущее
                    level = null
                )

                _state.value = _state.value.copy(
                    isSaving = false,
                    name = updated.name,
                    surname = updated.surname ?: "",
                    email = updated.email,
                    avatarUrl = updated.avatarUrl,
                    isSaved = true,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = mapError(e)
                )
            }
        }
    }

    fun updateAvatarUrl(url: String) {
        _state.value = _state.value.copy(avatarUrl = url)
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
