package com.example.fragment.module.user.model

import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.utils.WanHelper

class UserViewModel : BaseViewModel() {

    var userBean = UserBean()

    fun getUser() {
        WanHelper.getUser {
            userBean = it
        }
    }

    fun updateUser(userBean: UserBean) {
        WanHelper.setUser(userBean)
        this.userBean = userBean
    }

}