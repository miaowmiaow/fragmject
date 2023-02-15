package com.example.fragment.module.user.model

import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserState(
    val result: UserBean? = null,
    var time: Long = 0
) {
    fun getUserBean(): UserBean {
        return result ?: UserBean()
    }

    fun getUserId(): String {
        return getUserBean().id
    }
}

class UserViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(UserState())

    val uiState: StateFlow<UserState> = _uiState.asStateFlow()

    init {
        if (uiState.value.result == null) {
            getUser()
        }
    }

    fun getUserBean(): UserBean {
        return uiState.value.getUserBean()
    }

    fun getUserId(): String {
        return uiState.value.getUserId()
    }

    fun updateUserBean(userBean: UserBean) {
        WanHelper.setUser(userBean)
        _uiState.update {
            it.copy(result = userBean, time = System.currentTimeMillis())
        }
    }

    private fun getUser() {
        WanHelper.getUser { userBean ->
            _uiState.update {
                it.copy(result = userBean, time = System.currentTimeMillis())
            }
        }
    }

}