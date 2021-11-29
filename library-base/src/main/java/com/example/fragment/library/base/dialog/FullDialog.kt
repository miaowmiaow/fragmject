package com.example.fragment.library.base.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.fragment.library.base.R

open class FullDialog : BaseDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.FullDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            decorView.setPadding(0, 0, 0, 0)
            setWindowAnimations(R.style.AnimBottom)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

}