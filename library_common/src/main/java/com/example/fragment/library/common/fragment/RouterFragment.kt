package com.example.fragment.library.common.fragment

import android.content.Context
import com.example.fragment.library.base.component.fragment.BaseFragment
import com.example.fragment.library.common.activity.RouterActivity

open class RouterFragment : BaseFragment() {

    /**
     * 获取baseActivity方便调用navigation方法进行页面切换
     */
    lateinit var baseActivity: RouterActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseActivity = activity as RouterActivity
    }

    override fun onFirstLoad() {}

}