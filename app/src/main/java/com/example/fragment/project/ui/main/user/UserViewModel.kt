package com.example.fragment.project.ui.main.user

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.UserBean
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserState(
    var userBean: UserBean = UserBean(),
    var time: Long = 0
)

class UserViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(UserState())

    val uiState: StateFlow<UserState> = _uiState.asStateFlow()

    init {
        getUser()
    }

    private fun getUser() {
        WanHelper.getUser { userBean ->
            _uiState.update {
                it.userBean = userBean
                it.copy(time = System.currentTimeMillis())
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user/logout/json")
            //以get方式发起网络请求
            val response = get<HttpResponse>(request) { updateProgress(it) }
            if (response.errorCode == "0") {
                WanHelper.setUser(UserBean())
                _uiState.update {
                    it.userBean = UserBean()
                    it.copy(time = System.currentTimeMillis())
                }
            }


        }
    }
}