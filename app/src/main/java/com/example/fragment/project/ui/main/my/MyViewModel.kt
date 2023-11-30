package com.example.fragment.project.ui.main.my

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.User
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyUiState(
    var userBean: User = User(),
) {
    fun isLogin(): Boolean {
        return userBean.id.isNotBlank()
    }
}

class MyViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())

    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    fun getUser() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(userBean = WanHelper.getUser())
            }
        }
    }
}