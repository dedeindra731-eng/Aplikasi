package com.example.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.currentUser

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email dan kata sandi wajib diisi")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.loginWithEmail(email, pass)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    successMessage = "Selamat datang, ${user.displayName}!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Gagal masuk"
                )
            }
        }
    }

    fun register(name: String, email: String, pass: String, phone: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Lengkapi nama, email, dan kata sandi")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.registerWithEmail(name, email, pass, phone)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    successMessage = "Akun berhasil dibuat dengan Barcode ID unik!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Gagal mendaftar"
                )
            }
        }
    }

    fun quickSwitchDemoUser(index: Int) {
        viewModelScope.launch {
            authRepository.loginAsDemoUser(index)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun updateAvatar(avatarUrl: String) {
        viewModelScope.launch {
            val result = authRepository.updateAvatar(avatarUrl)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    user = it,
                    successMessage = "Foto profil berhasil diperbarui!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = err.localizedMessage ?: "Gagal memperbarui foto profil"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
