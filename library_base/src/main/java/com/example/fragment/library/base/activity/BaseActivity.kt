package com.example.fragment.library.base.activity

import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fragment.library.base.R
import com.example.fragment.library.base.utils.DBHelper
import com.example.fragment.library.base.view.TipsView
import com.example.fragment.library.base.utils.StatusBarUtils

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var tipsView: TipsView

    private val onBackPressedListeners: MutableMap<String, OnBackPressedListener> = HashMap()

    private var exitTime = 0L

    override fun setContentView(view: View) {
        //添加顶部提示view
        tipsView = TipsView(view.context)
        val frameLayout = FrameLayout(view.context)
        frameLayout.addView(view)
        frameLayout.addView(
            tipsView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).also { layoutParams ->
                layoutParams.topMargin = StatusBarUtils.getStatusBarHeight(view.context)
            }
        )
        super.setContentView(frameLayout)
    }

    override fun onBackPressed() {
        for (name in onBackPressedListeners.keys) {
            val listener = onBackPressedListeners[name]
            if (verifyFragment(name) && listener != null && listener.onBackPressed()) {
                return
            }
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            if (System.currentTimeMillis() - exitTime > 2000) {
                exitTime = System.currentTimeMillis()
                val msg = getString(R.string.one_more_press_2_back)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            } else {
                moveTaskToBack(true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DBHelper.closeDatabase()
    }

    fun showTips(text: String?) {
        tipsView.setMessage(text)
        tipsView.show()
    }

    fun alwaysShowTips(text: String?) {
        tipsView.setMessage(text)
        tipsView.alwaysShow()
    }

    fun dismissTips() {
        tipsView.dismiss()
    }

    /**
     * 验证目标Fragment是否为当前显示Fragment
     *
     * @param fragmentName Fragment
     * @return boolean
     */
    private fun verifyFragment(fragmentName: String): Boolean {
        return supportFragmentManager.findFragmentByTag(fragmentName) != null
    }

    /**
     * 注册返回键监听事件
     * 注册成功后拦截返回键事件并传递给监听者
     *
     * @param fragmentName Fragment
     * @param listener     OnBackPressedListener
     */
    fun registerOnBackPressedListener(
        fragmentName: String,
        listener: OnBackPressedListener
    ) {
        onBackPressedListeners[fragmentName] = listener
    }

    /**
     * 移除返回键监听事件
     *
     * @param fragmentName Fragment
     */
    fun removerOnBackPressedListener(fragmentName: String) {
        onBackPressedListeners.remove(fragmentName)
    }

}

/**
 * 返回键监听事件
 */
interface OnBackPressedListener {
    fun onBackPressed(): Boolean
}