package com.example.miaow.base.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import com.example.miaow.base.R

open class BottomDialog : BaseDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BaseDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            setWindowAnimations(R.style.AnimBottom)
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

}