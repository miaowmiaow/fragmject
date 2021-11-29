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

    /**
     * 获取头像
     */
    fun getAvatar() {
        WanHelper.getAvatar { avatarResult.postValue(it) }
    }

    /**
     * 获取用户信息
     */
    fun getUser() {
        WanHelper.getUser { userResult.postValue(it) }
    }

    /**
     * 获取显示模式
     */
    fun getUIMode() {
        WanHelper.getUIMode { uiModeResult.postValue(it) }
    }

    /**
     * 获取屏幕录制状态
     */
    fun getScreenRecordStatus() {
        WanHelper.getScreenRecordStatus { screenRecordStatusResult.postValue(it) }
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
            val response = get<HttpResponse>(request) { progress(it) }
            //通过LiveData通知界面更新
            logoutResult.postValue(response)
        }
    }

}