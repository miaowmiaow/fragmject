package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper

class UserViewModel : BaseViewModel() {

    val avatarResult = MutableLiveData<String>()
    val userResult = MutableLiveData<UserBean>()

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

}