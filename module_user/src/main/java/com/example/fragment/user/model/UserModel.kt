package com.example.fragment.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.library.common.bean.LoginBean
import com.example.fragment.library.common.bean.RegisterBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserModel : BaseViewModel() {

    val loginResult = MutableLiveData<LoginBean>()
    val registerResult = MutableLiveData<RegisterBean>()
    val logoutResult = MutableLiveData<HttpResponse>()

    fun login(username: String, password: String) {
        viewModelScope.launch(Dispatchers.Main) {
            loginResult.postValue(
                post(
                    HttpRequest("user/login")
                        .putParam("username", username)
                        .putParam("password", password)
                )
            )
        }
    }

    fun register(username: String, password: String, repassword: String) {
        viewModelScope.launch(Dispatchers.Main) {
            registerResult.postValue(
                post(
                    HttpRequest("user/register")
                        .putParam("username", username)
                        .putParam("password", password)
                        .putParam("repassword", repassword)
                )
            )
        }
    }

    fun logout(){
        viewModelScope.launch(Dispatchers.Main) {
            logoutResult.postValue(get(HttpRequest("user/logout/json")))
        }
    }

}