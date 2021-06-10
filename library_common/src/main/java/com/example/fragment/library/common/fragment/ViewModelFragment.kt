package com.example.fragment.library.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.bus.SimpleLiveBus
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus

/**
 * ViewModel和ViewBinding注册Fragment
 */
abstract class ViewModelFragment<VB : ViewBinding, VM : ViewModel> : RouterFragment() {

    lateinit var viewModel: VM

    private var _binding: VB? = null
    val binding get() = _binding!!

    abstract fun setViewBinding(): (LayoutInflater) -> VB
    
    abstract fun setViewModel(): Class<VM>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this as ViewModelStoreOwner).get(setViewModel())
        _binding = setViewBinding().invoke(inflater)
        binding.root.isClickable = true
        binding.root.isFocusable = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //通过LiveDataBus观察UserBean的变化，从而通知页面刷新
        SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).observe(this, { userBean ->
            onUserStatusUpdate(userBean)
        })
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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