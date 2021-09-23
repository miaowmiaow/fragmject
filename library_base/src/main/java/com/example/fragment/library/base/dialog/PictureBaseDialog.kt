package com.example.fragment.library.base.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import com.example.fragment.library.base.R

open class PictureBaseDialog : BaseDialog() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog?.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        super.onActivityCreated(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullDialog)
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

}
