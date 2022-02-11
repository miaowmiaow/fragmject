package com.example.fragment.library.common.model

import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper

class UserViewModel : BaseViewModel() {

    val userResult = MutableLiveData<UserBean>()

    fun getUser() {
        WanHelper.getUser {
            userResult.postValue(it)
        }
    }

    fun updateUser(userBean: UserBean) {
        WanHelper.setUser(userBean)
        userResult.postValue(userBean)
    }

}