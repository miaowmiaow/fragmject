package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.LoginBean
import com.example.fragment.library.common.bean.RegisterBean
import kotlinx.coroutines.launch

class UserLoginViewModel : BaseViewModel() {

    val loginResult = MutableLiveData<LoginBean>()
    val registerResult = MutableLiveData<RegisterBean>()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val request = HttpRequest("user/login")
                .putParam("username", username)
                .putParam("password", password)
            val response = post<LoginBean>(request) { progress(it) }
            loginResult.postValue(response)
        }
    }

    fun register(username: String, password: String, repassword: String) {
        viewModelScope.launch {
            val request = HttpRequest("user/register")
                .putParam("username", username)
                .putParam("password", password)
                .putParam("repassword", repassword)
            val response = post<RegisterBean>(request) { progress(it) }
            registerResult.postValue(response)
        }
    }

}