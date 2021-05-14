package com.example.fragment.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.bean.LoginBean
import com.example.fragment.library.common.bean.RegisterBean
import com.example.fragment.library.common.model.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserModel : BaseViewModel() {

    val loginResult = MutableLiveData<LoginBean>()
    val registerResult = MutableLiveData<RegisterBean>()
    val logoutResult = MutableLiveData<HttpResponse>()
    val shareArticleResult = MutableLiveData<HttpResponse>()

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

    fun logout() {
        viewModelScope.launch(Dispatchers.Main) {
            logoutResult.postValue(get(HttpRequest("user/logout/json")))
        }
    }

    fun shareArticle(title: String, link: String) {
        viewModelScope.launch(Dispatchers.Main) {
            registerResult.postValue(
                post(
                    HttpRequest("lg/user_article/add/json")
                        .putParam("title", title)
                        .putParam("link", link)
                )
            )
        }
    }

}