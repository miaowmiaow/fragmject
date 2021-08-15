package com.example.fragment.library.base.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

open class BaseDialog : DialogFragment() {

    private lateinit var manager: FragmentManager

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0.5F)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
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