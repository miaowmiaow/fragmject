package com.example.fragment.library.base.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.fragment.library.base.R

open class BottomDialog : BaseDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BaseDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            setWindowAnimations(R.style.AnimBottom)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

}