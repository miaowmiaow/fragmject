package com.example.fragment.library.common.fragment

import android.content.Context
import com.example.fragment.library.base.component.activity.BaseActivity
import com.example.fragment.library.base.component.fragment.BaseFragment
import com.example.fragment.library.common.activity.RouterActivity

open class RouterFragment : BaseFragment() {

    lateinit var baseActivity: RouterActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseActivity = activity as RouterActivity
    }

    override fun onFirstLoad() {}

}