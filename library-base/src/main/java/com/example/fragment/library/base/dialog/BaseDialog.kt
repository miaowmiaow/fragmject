package com.example.fragment.library.base.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

open class BaseDialog : DialogFragment() {

    lateinit var manager: FragmentManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            setDimAmount(0F)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
        dismiss()
        show(manager, this::class.java.canonicalName)
    }

}