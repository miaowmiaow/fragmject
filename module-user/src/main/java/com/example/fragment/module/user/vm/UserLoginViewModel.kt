package com.example.fragment.module.user.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.common.bean.LoginBean
import com.example.fragment.library.common.bean.RegisterBean
import kotlinx.coroutines.launch

class UserLoginViewModel : BaseViewModel() {

    private val loginResult = MutableLiveData<LoginBean>()

    fun loginResult(): LiveData<LoginBean> {
        return loginResult
    }

    fun login(username: String, password: String) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user/login")
                .putParam("username", username)
                .putParam("password", password)
            //以post方式发起网络请求
            val response = post<LoginBean>(request) { updateProgress(it) }
            //通过LiveData通知界面更新
            loginResult.postValue(response)
        }
    }

    private val registerResult = MutableLiveData<RegisterBean>()

    fun registerResult(): LiveData<RegisterBean> {
        return registerResult
    }

    fun register(username: String, password: String, repassword: String) {
        viewModelScope.launch {
            val request = HttpRequest("user/register")
                .putParam("username", username)
                .putParam("password", password)
                .putParam("repassword", repassword)
            val response = post<RegisterBean>(request) { updateProgress(it) }
            registerResult.postValue(response)
        }
    }

    private val logoutResult = MutableLiveData<HttpResponse>()

    fun logoutResult(): LiveData<HttpResponse> {
        return logoutResult
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
            //通过LiveData通知界面更新
            logoutResult.postValue(response)
        }
    }

}