package com.example.miaow.base.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.miaow.base.R

open class RightDialog : BaseDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BaseDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

}