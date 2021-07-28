package com.example.fragment.library.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.bus.SimpleLiveDataBus
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus

/**
 * ViewModel和ViewBinding注册Fragment
 */
abstract class ViewBindingFragment<VB : ViewBinding> : RouterFragment() {

    private var _binding: VB? = null
    val binding get() = _binding!!

    abstract fun setViewBinding(): (LayoutInflater, ViewGroup?, Boolean) -> VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = setViewBinding().invoke(inflater, container, false)
        binding.root.isClickable = true
        binding.root.isFocusable = true
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        //通过LiveDataBus观察UserBean的变化，从而通知页面刷新
        SimpleLiveDataBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).observe(this, { userBean ->
            onUserStatusUpdate(userBean)
        })
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

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