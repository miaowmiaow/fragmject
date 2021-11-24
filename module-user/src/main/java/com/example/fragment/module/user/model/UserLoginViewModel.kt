package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.*
import kotlinx.coroutines.launch

class UserLoginViewModel : BaseViewModel() {

    val loginResult = MutableLiveData<LoginBean>()
    val registerResult = MutableLiveData<RegisterBean>()
    val logoutResult = MutableLiveData<HttpResponse>()

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

    fun logout() {
        viewModelScope.launch {
            val request = HttpRequest("user/logout/json")
            val response = get<HttpResponse>(request) { progress(it) }
            logoutResult.postValue(response)
        }
    }

}