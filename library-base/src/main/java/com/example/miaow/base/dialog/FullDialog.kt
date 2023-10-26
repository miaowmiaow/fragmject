package com.example.miaow.base.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import com.example.miaow.base.R

open class FullDialog : BaseDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

}