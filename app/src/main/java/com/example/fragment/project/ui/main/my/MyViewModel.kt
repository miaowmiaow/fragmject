package com.example.fragment.project.ui.main.my

import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.UserBean
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MyState(
    var isLoading: Boolean = false,
    var userBean: UserBean = UserBean(),
) {
    fun isLogin(): Boolean {
        return userBean.id.isNotBlank()
    }
}

class MyViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyState())

    val uiState: StateFlow<MyState> = _uiState.asStateFlow()

    init {
        getUser()
    }

    fun getUser() {
        _uiState.update { it.copy(isLoading = true) }
        WanHelper.getUser { userBean ->
            _uiState.update {
                it.userBean = userBean
                it.copy(isLoading = false)
            }
        }
    }
}