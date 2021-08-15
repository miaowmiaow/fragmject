package com.example.fragment.library.common.fragment

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.fragment.library.base.bus.LiveDataBus
import com.example.fragment.library.base.fragment.BaseFragment
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus

open class RouterFragment : BaseFragment() {

    /**
     * 获取baseActivity方便调用navigation方法进行页面切换
     */
    lateinit var baseActivity: RouterActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseActivity = activity as RouterActivity
    }

    override fun onStart() {
        super.onStart()
        //通过LiveDataBus观察UserBean的变化，从而通知页面刷新
        LiveDataBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).observe(this, { userBean ->
            onUserStatusUpdate(userBean)
        })
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun onFirstLoad() {}

    /**
     * 用户状态更新方法
     *      当UserBean发生变化时该方法调用
     */
    open fun onUserStatusUpdate(userBean: UserBean) {}

    private fun hideInputMethod() {
        val inputMethodManager =
            baseActivity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = baseActivity.currentFocus ?: return
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}