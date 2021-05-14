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
import java.lang.reflect.ParameterizedType

abstract class ViewModelFragment<VB : ViewBinding, VM : ViewModel> :
    RouterFragment() {

    lateinit var binding: VB
    lateinit var viewModel: VM

    abstract fun setViewBinding(inflater: LayoutInflater): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val type = javaClass.genericSuperclass
        val clazz = if (type is ParameterizedType) {
            type.actualTypeArguments[1] as Class<VM>
        } else {
            throw Exception("must has ParameterizedTypes")
        }
        viewModel = ViewModelProvider(this as ViewModelStoreOwner).get(clazz)
        binding = setViewBinding(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).observe(this, { userBean ->
            onUserStatusUpdate(userBean)
        })
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    open fun onUserStatusUpdate(userBean: UserBean) {

    }

    private fun hideInputMethod() {
        val inputMethodManager =
            baseActivity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = baseActivity.currentFocus ?: return
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}