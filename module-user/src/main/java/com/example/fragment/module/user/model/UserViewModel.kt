package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.utils.WanHelper

class UserViewModel : BaseViewModel() {

    val localAvatarResult = MutableLiveData<String>()

    /**
     * 获取头像
     */
    fun getLocalAvatar() {
        WanHelper.getLocalAvatar { localAvatarResult.postValue(it) }
    }

}