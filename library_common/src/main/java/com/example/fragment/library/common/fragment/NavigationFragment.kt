package com.example.fragment.library.common.fragment

import android.content.Context
import com.example.fragment.library.base.component.fragment.BaseFragment
import com.example.fragment.library.common.activity.RouterActivity

open class NavigationFragment : BaseFragment() {

    lateinit var routerActivity: RouterActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        routerActivity = activity as RouterActivity
    }

    override fun onFirstLoad() {}
}