package com.example.fragment.library.base.activity

import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.fragment.library.base.utils.StatusBarUtils
import com.example.fragment.library.base.view.TipsView

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var tipsView: TipsView

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

}
