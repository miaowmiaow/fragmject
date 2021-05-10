package com.example.fragment.library.common.fragment

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.example.fragment.library.base.component.fragment.BaseFragment
import com.example.fragment.library.common.activity.RouterActivity

open class RouterFragment : BaseFragment() {

    lateinit var fragmentActivity: FragmentActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentActivity = activity as FragmentActivity
    }

    override fun onFirstLoad() {}

    fun getRouterActivity(): RouterActivity {
        return fragmentActivity as RouterActivity
    }
}