package com.example.miaow.base.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.miaow.base.databinding.StandardDialogBinding

class StandardDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): StandardDialog {
            return StandardDialog()
        }
    }

    private lateinit var binding: StandardDialogBinding

    private var listener: OnDialogClickListener? = null
    private var title: String? = null
    private var content: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = StandardDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0.5F)
        }
        title?.apply {
            binding.title.text = this
            binding.title.visibility = View.VISIBLE
        }
        content?.apply {
            binding.content.text = this
            binding.content.visibility = View.VISIBLE
        }
        binding.confirm.setOnClickListener {
            dismiss()
            listener?.apply {
                onConfirm(this@StandardDialog)
            }
        }
        binding.cancel.setOnClickListener {
            dismiss()
            listener?.apply {
                onCancel(this@StandardDialog)
            }
        }
    }

    fun setTitle(text: String?): StandardDialog {
        this.title = text
        return this
    }

    fun setContent(text: String?): StandardDialog {
        this.content = text
        return this
    }

    fun setOnDialogClickListener(listener: OnDialogClickListener): StandardDialog {
        this.listener = listener
        return this
    }

    interface OnDialogClickListener {
        fun onConfirm(dialog: StandardDialog)
        fun onCancel(dialog: StandardDialog)
    }

}

fun Context.showStandardDialog(
    title: String? = null,
    content: String? = null,
    confirm: (() -> Unit)? = null,
    cancel: (() -> Unit)? = null
) {
    StandardDialog
        .newInstance()
        .setTitle(title)
        .setContent(content)
        .setOnDialogClickListener(object :
            StandardDialog.OnDialogClickListener {
            override fun onConfirm(dialog: StandardDialog) {
                confirm?.invoke()
            }

            override fun onCancel(dialog: StandardDialog) {
                cancel?.invoke()
            }

        })
        .show(this)
}