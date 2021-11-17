package com.example.fragment.library.common.fragment

import android.content.Context
import com.example.fragment.library.base.fragment.BaseFragment
import com.example.fragment.library.common.activity.RouterActivity

abstract class RouterFragment : BaseFragment() {

    /**
     * 获取baseActivity方便调用navigation方法进行页面切换
     */
    lateinit var activity: RouterActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as RouterActivity
    }

}