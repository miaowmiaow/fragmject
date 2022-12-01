package com.example.fragment.library.common.fragment

import android.content.Context
import android.os.Bundle
import com.example.fragment.library.base.fragment.BaseFragment
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.constant.Router

abstract class RouterFragment : BaseFragment() {

    /**
     * 获取RouterActivity方便调用navigation进行页面切换
     */
    private var routerActivity: RouterActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RouterActivity) {
            routerActivity = context
        }
    }

    fun onBackPressed(){
        routerActivity?.onBackPressedDispatcher?.onBackPressed()
    }

    fun navigation(name: Router, bundle: Bundle? = null) {
        routerActivity?.navigation(name, bundle)
    }

    fun alwaysShowTips(text: String?) {
        routerActivity?.alwaysShowTips(text)
    }

    fun showTips(text: String?) {
        routerActivity?.showTips(text)
    }

    fun dismissTips() {
        routerActivity?.dismissTips()
    }

    /**
     * 网络请求code处理
     * 封装在网络框架中扩展太差，暂时写在此处待优化
     */
    fun <T : HttpResponse> httpParseSuccess(result: T?, success: ((T) -> Unit)) {
        result?.let {
            when (it.errorCode) {
                "0" -> success.invoke(it)
                "-1001" -> navigation(Router.USER_LOGIN)
                else -> showTips(it.errorMsg)
            }
        }
    }

}