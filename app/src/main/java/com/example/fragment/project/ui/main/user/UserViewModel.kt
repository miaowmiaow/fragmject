package com.example.fragment.project.ui.main.user

import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.UserBean
import com.example.fragment.project.utils.WanHelper
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

    fun updateUserBean(userBean: UserBean?) {
        userBean?.let {
            WanHelper.setUser(userBean)
            _uiState.update {
                it.copy(result = userBean, time = System.currentTimeMillis())
            }
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