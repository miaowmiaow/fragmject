package com.example.fragment.module.user.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper

class UserViewModel : BaseViewModel() {

    private val userResult: MutableLiveData<UserBean> by lazy {
        MutableLiveData<UserBean>().also {
            getUser()
        }
    }

    fun userResult(): LiveData<UserBean> {
        return userResult
    }

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

    private fun getUser() {
        WanHelper.getUser {
            userResult.postValue(it)
        }
    }

}