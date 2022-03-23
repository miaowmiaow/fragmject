package com.example.fragment.library.base.dialog

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.example.fragment.library.base.R

open class FullDialog : RightDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.FullDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

}