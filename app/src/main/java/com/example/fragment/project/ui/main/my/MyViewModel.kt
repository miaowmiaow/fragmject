package com.example.fragment.project.ui.main.my

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.database.user.User
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MyUiState {
    val user: User

    data class NoUser(
        override var user: User = User(id = "", username = "", nickname = "", darkTheme = false),
    ) : MyUiState
}

class MyViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyUiState.NoUser())

    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WanHelper.getUser().collect { user ->
                _uiState.update { state ->
                    if (user != null) {
                        state.copy(user = user)
                    } else {
                        state.copy(user = MyUiState.NoUser().user)
                    }
                }
            }
        }
    }

}