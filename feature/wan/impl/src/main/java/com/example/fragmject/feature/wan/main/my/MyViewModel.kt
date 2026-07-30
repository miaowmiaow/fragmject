package com.example.fragmject.feature.wan.main.my

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.core.database.store.UserStore
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyUiState {
    val user: UserEntity
    data class NoUser(
        override val user: UserEntity = UserEntity(id = 0, username = "", nickname = ""),
    ) : MyUiState
}

@HiltViewModel
class MyViewModel @Inject constructor() : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyUiState.NoUser())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            UserStore.getUser().collect { user ->
                _uiState.update {
                    when (it) {
                        is MyUiState.NoUser -> it.copy(user = user ?: UserEntity(id = 0, username = "", nickname = ""))
                    }
                }
            }
        }
    }
}