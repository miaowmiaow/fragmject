package com.example.fragment.library.base.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View

open class TransparentDialog : BaseDialog() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            setDimAmount(0F)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        super.onViewCreated(view, savedInstanceState)
    }

}