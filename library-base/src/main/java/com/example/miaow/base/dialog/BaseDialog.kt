package com.example.miaow.base.dialog

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

open class BaseDialog : DialogFragment() {

    lateinit var manager: FragmentManager

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            try {
                super.show(manager, tag)
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            try {
                super.showNow(manager, tag)
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }
    }

    fun show(manager: FragmentManager) {
        this.manager = manager
        show(manager, this::class.java.canonicalName)
    }

    fun show(context: Context) {
        if (context is AppCompatActivity) {
            show(context.supportFragmentManager)
        }
    }

    /**
     * 设置状态栏
     */
    fun setStatusBar(view: View, @ColorInt color: Int, isLight: Boolean) {
        val window: Window = try {
            (context as Activity).window
        } catch (e: Exception) {
            ((context as ContextThemeWrapper).baseContext as Activity).window
        }
        window.statusBarColor = color //设置状态栏底色
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = isLight //设置状态栏亮起
        }
    }

    /**
     * 设置导航栏
     */
    fun setNavigationBar(view: View, @ColorInt color: Int, isLight: Boolean) {
        val window: Window = try {
            (context as Activity).window
        } catch (e: Exception) {
            ((context as ContextThemeWrapper).baseContext as Activity).window
        }
        window.navigationBarColor = color //设置导航栏底色
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightNavigationBars = isLight //设置导航栏亮起
        }
    }

}