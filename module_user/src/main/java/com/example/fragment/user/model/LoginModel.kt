package com.example.fragment.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.user.bean.LoginBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginModel : BaseViewModel() {

    val loginResult = MutableLiveData<LoginBean>()

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

}