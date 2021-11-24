package com.example.fragment.library.base.activity

import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.fragment.library.base.utils.StatusBarUtils
import com.example.fragment.library.base.view.ProgressView
import com.example.fragment.library.base.view.TipsView

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var tipsView: TipsView
    private lateinit var progressView: ProgressView

    override fun setContentView(view: View) {
        //添加顶部提示view
        tipsView = TipsView(view.context)
        //添加进度条view
        progressView = ProgressView(view.context)
        val layout = RelativeLayout(view.context)
        layout.addView(view)
        layout.addView(
            tipsView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).also { layoutParams ->
                layoutParams.topMargin = StatusBarUtils.getStatusBarHeight(view.context)
            }
        )
        layout.addView(
            progressView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).also { layoutParams ->
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            }
        )
        super.setContentView(layout)
    }

    fun showTips(text: String?) {
        if (!text.isNullOrBlank()) {
            tipsView.setMessage(text)
            tipsView.show()
        }
    }

    fun alwaysShowTips(text: String?) {
        if (!text.isNullOrBlank()) {
            tipsView.setMessage(text)
            tipsView.alwaysShow()
        }
    }

    fun dismissTips() {
        tipsView.dismiss()
    }

    fun showProgress() {
        progressView.show()
    }

    fun dismissProgress() {
        progressView.dismiss()
    }

}
