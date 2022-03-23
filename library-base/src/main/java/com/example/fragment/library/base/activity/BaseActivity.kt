package com.example.fragment.library.base.activity

import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.fragment.library.base.view.TipsView
import com.example.fragment.library.base.view.statusBarHeight

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var tipsView: TipsView

    override fun setContentView(view: View) {
        //添加顶部提示view
        tipsView = TipsView(view.context)
        val layout = RelativeLayout(view.context)
        layout.addView(view)
        layout.addView(tipsView, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.topMargin = view.statusBarHeight()
        })
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

}
