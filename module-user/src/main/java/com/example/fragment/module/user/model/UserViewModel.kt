package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper

class UserViewModel : BaseViewModel() {

    val userResult = MutableLiveData<UserBean>()

    fun getUserId(): String {
        return userResult.value?.id ?: ""
    }

    fun getUserBean(): UserBean {
        return userResult.value ?: UserBean()
    }

    fun updateUserBean(userBean: UserBean) {
        userResult.postValue(userBean)
        WanHelper.setUser(userBean)
    }

    fun getUser() {
        WanHelper.getUser {
            userResult.postValue(it)
        }
    }

}