package com.example.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val scannedSuccessUser: User? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class ContactsViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    val filteredContacts: StateFlow<List<User>> = combine(
        contactRepository.contacts,
        _searchQuery
    ) { contacts, query ->
        if (query.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.barcodeId.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addFriendFromBarcode(scannedRaw: String, onFinished: (Boolean, User?) -> Unit) {
        viewModelScope.launch {
            val result = contactRepository.addContactByBarcode(scannedRaw)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    scannedSuccessUser = user,
                    infoMessage = "Berhasil menambahkan ${user.displayName} sebagai teman!",
                    errorMessage = null
                )
                onFinished(true, user)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Gagal memindai barcode: ${err.message}"
                )
                onFinished(false, null)
            }
        }
    }

    fun clearInfo() {
        _uiState.value = _uiState.value.copy(
            scannedSuccessUser = null,
            infoMessage = null,
            errorMessage = null
        )
    }
}
