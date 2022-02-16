package com.example.fragment.library.common.fragment

import android.content.Context
import com.example.fragment.library.base.fragment.BaseFragment
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Router

abstract class RouterFragment : BaseFragment() {

    /**
     * 获取RouterActivity方便调用navigation进行页面切换
     */
    lateinit var activity: RouterActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as RouterActivity
    }

    /**
     * 网络请求结果处理（不建议封装在网络请求框架中）
     */
    fun <T : HttpResponse> wanSuccessCallback(result: T, success: (() -> Unit)) {
        when (result.errorCode) {
            "0" -> success.invoke()
            "-1001" -> activity.navigation(Router.USER_LOGIN)
            else -> activity.showTips(result.errorMsg)
        }
    }

}