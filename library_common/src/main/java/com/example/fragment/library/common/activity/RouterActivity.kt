package com.example.fragment.library.common.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.fragment.library.base.component.activity.BaseActivity
import com.example.fragment.library.base.utils.FragmentHelper
import com.example.fragment.library.common.constant.NavMode
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper

/**
 * 路由类，方便模块之间调用
 */
abstract class RouterActivity : BaseActivity() {

    private var currFragment: Class<out Fragment>? = null

    abstract fun frameLayoutId(): Int

    /**
     * 导航方法，根据路由名跳转切换Fragment
     */
    abstract fun navigation(
        name: Router,
        bundle: Bundle? = null,
        navMode: NavMode = NavMode.SWITCH,
    )

    fun switcher(
        clazz: Class<out Fragment>,
        bundle: Bundle? = null,
        navMode: NavMode
    ) {
        if (navMode == NavMode.SWITCH) {
            currFragment = FragmentHelper.switcher(
                supportFragmentManager,
                frameLayoutId(),
                currFragment,
                clazz,
                bundle,
                true
            )
        } else if (navMode == NavMode.POP_BACK_STACK) {
            currFragment = FragmentHelper.pop(supportFragmentManager, clazz)
        }
    }

    /**
     * 设置夜间模式方法
     */
    fun initUIMode() {
        WanHelper.getUIMode().observe(this, {
            when (it) {
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        })
    }

}