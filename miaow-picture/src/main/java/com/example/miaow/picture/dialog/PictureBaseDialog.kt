package com.example.miaow.picture.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

open class PictureBaseDialog : DialogFragment() {

    lateinit var manager: FragmentManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            decorView.setPadding(0, 0, 0, 0)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            try {
                super.show(manager, tag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            try {
                super.showNow(manager, tag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun show(manager: FragmentManager) {
        this.manager = manager
        show(manager, this::class.java.canonicalName)
    }

}
