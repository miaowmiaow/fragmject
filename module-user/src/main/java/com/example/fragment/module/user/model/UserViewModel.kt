package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper
import kotlinx.coroutines.launch

class UserViewModel : BaseViewModel() {

    val avatarResult = MutableLiveData<String>()
    val userResult = MutableLiveData<UserBean>()
    val uiModeResult = MutableLiveData<Int>()
    val screenRecordStatusResult = MutableLiveData<Int>()
    val logoutResult = MutableLiveData<HttpResponse>()

    fun getAvatar() {
        WanHelper.getAvatar { avatarResult.postValue(it) }
    }

    fun getUser() {
        WanHelper.getUser { userResult.postValue(it) }
    }

    fun getUIMode() {
        WanHelper.getUIMode { uiModeResult.postValue(it) }
    }

    fun getScreenRecordStatus() {
        WanHelper.getScreenRecordStatus { screenRecordStatusResult.postValue(it) }
    }

    fun logout() {
        viewModelScope.launch {
            val request = HttpRequest("user/logout/json")
            val response = get<HttpResponse>(request) { progress(it) }
            logoutResult.postValue(response)
        }
    }

}