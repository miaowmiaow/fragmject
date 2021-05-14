package com.example.fragment.library.base.component.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import com.example.fragment.library.base.R

class FullDialog : BaseDialog() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog?.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        super.onActivityCreated(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullDialog)
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            decorView.setPadding(0, 0, 0, 0)
            setWindowAnimations(R.style.AnimBottom)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
    }

}