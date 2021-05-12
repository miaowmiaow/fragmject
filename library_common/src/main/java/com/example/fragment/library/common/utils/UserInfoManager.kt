package com.example.fragment.library.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.fragment.library.base.db.SimpleDBHelper
import com.example.fragment.library.common.bean.UserBean
import com.google.gson.Gson

object UserInfoManager {

    private const val USER = "user"

    fun setUser(userBean: UserBean) {
        SimpleDBHelper.set(USER, userBean.toJson())
    }

    fun getUser(): LiveData<UserBean> {
        return Transformations.map(SimpleDBHelper.get(USER)) {
            try {
                Gson().fromJson(it, UserBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                UserBean::class.java.newInstance()
            }
        }

    }

}