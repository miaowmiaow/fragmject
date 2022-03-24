package com.example.fragment.library.base.dialog

import android.app.Activity
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

open class BaseDialog : DialogFragment() {

    lateinit var manager: FragmentManager

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            try {
                super.show(manager, tag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            try {
                super.showNow(manager, tag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun show(manager: FragmentManager) {
        this.manager = manager
        show(manager, this::class.java.canonicalName)
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
        WindowCompat.getInsetsController(window, view)?.apply {
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
        WindowCompat.getInsetsController(window, view)?.apply {
            isAppearanceLightNavigationBars = isLight //设置导航栏亮起
        }
    }

}