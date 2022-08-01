package com.example.miaow.picture.editor.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import com.example.fragment.library.base.dialog.FullDialog
import com.example.miaow.picture.databinding.PictureTextDialogBinding
import com.example.miaow.picture.editor.bean.StickerAttrs
import com.example.miaow.picture.editor.utils.ColorUtils

class PictureTextDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PictureTextDialog {
            return PictureTextDialog()
        }
    }

    private var _binding: PictureTextDialogBinding? = null
    private val binding get() = _binding!!
    private var _callback: PictureTextCallback? = null
    private val textColors: MutableList<RelativeLayout> = arrayListOf()
    private var _attrs: StickerAttrs? = null
    private val attrs get() = _attrs!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PictureTextDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _callback = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(1f)
        }
        textColors.add(binding.textWhite)
        textColors.add(binding.textBlack)
        textColors.add(binding.textRed)
        textColors.add(binding.textYellow)
        textColors.add(binding.textGreen)
        textColors.add(binding.textBlue)
        textColors.add(binding.textPurple)
        textColors.forEachIndexed { index, colorView ->
            colorView.setOnClickListener {
                selectedColor(colorView)
                binding.editText.setTextColor(ColorUtils.colorful[index])
            }
        }
        _attrs?.apply {
            binding.editText.setText(description)
        }
        binding.textBack.setOnClickListener {
            hideSoftInput(binding.editText)
            binding.editText.isFocusable = false
            dismiss()
        }
        binding.textFinish.setOnClickListener {
            hideSoftInput(binding.editText)
            binding.editText.isFocusable = false
            val description = binding.editText.text.toString()
            if (description.isNotBlank()) {
                if (_attrs == null) {
                    _attrs = StickerAttrs(saveBitmap())
                } else {
                    attrs.bitmap = saveBitmap()
                }
                attrs.description = description
                _callback?.onFinish(attrs)
            }
            dismiss()
        }
        binding.editText.postDelayed({
            binding.editText.isFocusable = true
            binding.editText.isFocusableInTouchMode = true
            binding.editText.requestFocus()
            showSoftInput(binding.editText)
        }, 250)
    }

    private fun selectedColor(view: View) {
        textColors.forEach {
            it.isSelected = false
        }
        view.isSelected = true
    }

    private fun showSoftInput(view: View) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE).apply {
            if (this is InputMethodManager) {
                showSoftInput(view, 0)
            }
        }
    }

    private fun hideSoftInput(view: View) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE).apply {
            if (this is InputMethodManager) {
                hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun saveBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            binding.editText.width,
            binding.editText.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        binding.editText.draw(canvas)
        return bitmap
    }

    fun setStickerAttrs(attrs: StickerAttrs?): PictureTextDialog {
        this._attrs = attrs
        return this
    }

    fun setPictureTextCallback(callback: PictureTextCallback): PictureTextDialog {
        this._callback = callback
        return this
    }

}

interface PictureTextCallback {
    fun onFinish(attrs: StickerAttrs)
}