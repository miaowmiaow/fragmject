package com.example.fragmject.feature.home.ui.my

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.User
import com.example.fragmject.core.domain.repository.UserRepository
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyUiState {
    val user: User
    data class NoUser(
        override val user: User = User(id = 0, username = "", nickname = ""),
    ) : MyUiState
}

@HiltViewModel
class MyViewModel @Inject constructor(
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState.NoUser())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepo.observeCurrentUser().collect { user ->
                _uiState.update {
                    it.copy(user = user ?: User(id = 0, username = "", nickname = ""))
                }
            }
        }
    }
}