package com.example.fragment.library.common.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.fragment.library.base.component.activity.BaseActivity
import com.example.fragment.library.base.utils.FragmentHelper
import com.example.fragment.library.common.constant.NavMode
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.utils.WanHelper

abstract class RouterActivity : BaseActivity() {

    private var curFragment: Class<out Fragment>? = null

    abstract fun frameLayoutId(): Int

    abstract fun navigation(
        name: Router,
        bundle: Bundle? = null,
        onBack: Boolean = true,
        navMode: NavMode = NavMode.SWITCH,
    )

    fun switcher(
        clazz: Class<out Fragment>,
        bundle: Bundle? = null,
        addToBackStack: Boolean,
        navMode: NavMode
    ) {
        if (navMode == NavMode.SWITCH) {
            curFragment =
                FragmentHelper.switcher(
                    supportFragmentManager,
                    frameLayoutId(),
                    curFragment,
                    clazz,
                    bundle,
                    addToBackStack
                )
        } else if (navMode == NavMode.POP_BACK_STACK) {
            curFragment = FragmentHelper.pop(supportFragmentManager, clazz)
        }
    }

    fun initUIMode() {
        WanHelper.getUIMode().observe(this, {
            when (it) {
                1 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                2 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                else -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        })
    }

}